import java.lang.*;

public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
 
    public FileSystem( int diskBlocks ) {
       // create superblock, and format disk with 64 inodes in default
       superblock = new SuperBlock( diskBlocks );
 
       // create directory, and register "/" in directory entry 0
       directory = new Directory( superblock.inodeBlocks );
 
       // file table is created, and store directory in the file table
       filetable = new FileTable( directory );
 
       // directory reconstruction
       FileTableEntry dirEnt = open( "/", "r" );
       int dirSize = fsize( dirEnt );
       if ( dirSize > 0 ) {
          byte[] dirData = new byte[dirSize];
          read( dirEnt, dirData );
          directory.bytes2directory( dirData );
       }
       close( dirEnt );
    }
 
    void sync( ) {
    }

    boolean format( int files ) {
        superblock.format(files);
        return true;
    }

    FileTableEntry open( String filename, String mode ) {
        FileTableEntry ftEnt = filetable.falloc(filename, mode);
        if(mode.equals("w")) {
            if(deallocAllBlocks(ftEnt) == false) {
                return null;
            }
        }
        return ftEnt;
    }

    boolean close( FileTableEntry ftEnt ) {
        return filetable.ffree(ftEnt);
    }

    int fsize( FileTableEntry ftEnt ) {
        if(ftEnt == null) {
            return -1;
        }
        return ftEnt.inode.length; 
    }

    int read( FileTableEntry ftEnt, byte[] buffer ) {
        if(ftEnt.mode != "r" && ftEnt.mode != "w+"){
            return -1;
        }

        int bufferIndex = 0;
        while(ftEnt.seekPtr < ftEnt.inode.length) {
            short blockNumber = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
            int blocksUsed = ftEnt.seekPtr / 512;
            byte[] rawBuffer = new byte[512];
            SysLib.rawread(blockNumber, rawBuffer);
            
            // Work out where we should start reading in the rawBuffer
            int rawBufferIndex = ftEnt.seekPtr % 512;
            while((bufferIndex < buffer.length) && (ftEnt.seekPtr < ftEnt.inode.length)) {
                buffer[bufferIndex] = rawBuffer[rawBufferIndex];
                bufferIndex++;
                rawBufferIndex++;
                ftEnt.seekPtr++;

                if((ftEnt.seekPtr / 512) > blocksUsed) {
                    break;
                }
            }
            if(bufferIndex == buffer.length) {
                break;
            }
        }

        return bufferIndex;
    }

    int write( FileTableEntry ftEnt, byte[] buffer ) {
        if(ftEnt.mode == "r") {
            return -1;
        }
        int bufferIndex = 0;
        while(bufferIndex < buffer.length) {
            int blocksUsed = ftEnt.seekPtr / 512;
            int block = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
            if(block == -1) {
                block = superblock.getFreeBlock();
                // No more disk space
                if(block == -1) {
                    return -1;
                }
                if(ftEnt.inode.setTargetBlock(ftEnt.seekPtr, (short)block) == -3) {
                    int indexBlock = superblock.getFreeBlock();
                    if(indexBlock == -1) {
                        return -1;
                    }
                    ftEnt.inode.setIndexBlock((short)indexBlock);
                    ftEnt.inode.setTargetBlock(ftEnt.seekPtr, (short)block);
                }
            }

            byte[] rawBuffer = new byte[512];
            SysLib.rawread(block, rawBuffer);

            // Work out where we should start reading in the rawBuffer
            int rawBufferIndex = ftEnt.seekPtr % 512;
            
            while(bufferIndex < buffer.length) {
                rawBuffer[rawBufferIndex] = buffer[bufferIndex];
                bufferIndex++;
                rawBufferIndex++;
                ftEnt.seekPtr++;

                if((ftEnt.seekPtr / 512) > blocksUsed) {
                    break;
                }
            }

            SysLib.rawwrite(block, rawBuffer);

            if(bufferIndex == buffer.length) {
                break;
            }
        }
        if(ftEnt.seekPtr > ftEnt.inode.length) {
            ftEnt.inode.length = ftEnt.seekPtr;
        }
        return bufferIndex;
    }

    private boolean deallocAllBlocks( FileTableEntry ftEnt ) {
        Inode node = ftEnt.inode;

        // Free all the direct pointers
        for(int i = 0; i < node.direct.length; i++) {
            if(node.direct[i] == -1) {
                continue;
            }

            superblock.returnBlock(node.direct[i]);
            node.direct[i] = -1;
        }

        // Free all the indirect pointers, if there are any
        short[] indirectBlocks = node.unsetIndexBlock();
        if(indirectBlocks != null) {
            for(int i = 0; i < indirectBlocks.length; i++) {
                superblock.returnBlock(indirectBlocks[i]);
            }
        }

        // Make sure we reset things to clean state
        ftEnt.seekPtr = 0;
        node.length = 0;
        node.flag = 0;
        node.toDisk(ftEnt.iNumber);

        return true;
    }

    boolean delete( String filename ) {
        // Check if file is open
        if(filetable.isFileOpen(filename)) {
            return false;
        }
        // Returns null if file does not exist
        FileTableEntry entryFile = open(filename, "r");
        if(entryFile == null) {
            return true;
        }
        // Frees all blocks associated with inode
        if(!deallocAllBlocks(entryFile)) {
            return false;
        }
        // Removes file from directory listing
        short iNumber = entryFile.iNumber;
        filetable.ffree(entryFile);
        directory.ifree(iNumber);
        return true;
    }
 
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;
 
    int seek( FileTableEntry ftEnt, int offset, int whence ) {
        int newPosition = 0;
        switch(whence) {
            case SEEK_SET:
            newPosition = offset;
            break;

            case SEEK_CUR:
            newPosition = ftEnt.seekPtr + offset;
            break;

            case SEEK_END:
            newPosition = ftEnt.inode.length + offset;
            break;
        }
        newPosition = Math.max(newPosition, 0);
        newPosition = Math.min(newPosition, ftEnt.inode.length);
        ftEnt.seekPtr = newPosition;
        return newPosition;
    }
 }
 