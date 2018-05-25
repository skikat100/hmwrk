public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    public FileSystem(int var1) {
        this.superblock = new SuperBlock(var1);
        this.directory = new Directory(this.superblock.inodeBlocks);
        this.filetable = new FileTable(this.directory);
        FileTableEntry var2 = this.open("/", "r");
        int var3 = this.fsize(var2);
        if (var3 > 0) {
            byte[] var4 = new byte[var3];
            this.read(var2, var4);
            this.directory.bytes2directory(var4);
        }

        this.close(var2);
    }

    void sync() {
        FileTableEntry var1 = this.open("/", "w");
        byte[] var2 = this.directory.directory2bytes();
        this.write(var1, var2);
        this.close(var1);
        this.superblock.sync();
    }

    boolean format(int var1) {
        while(!this.filetable.fempty()) {
            ;
        }

        this.superblock.format(var1);
        this.directory = new Directory(this.superblock.inodeBlocks);
        this.filetable = new FileTable(this.directory);
        return true;
    }

    FileTableEntry open(String var1, String var2) {
        FileTableEntry var3 = this.filetable.falloc(var1, var2);
        return var2 == "w" && !this.deallocAllBlocks(var3) ? null : var3;
    }

    boolean close(FileTableEntry var1) {
        synchronized(var1) {
            --var1.count;
            if (var1.count > 0) {
                return true;
            }
        }

        return this.filetable.ffree(var1);
    }

    int fsize(FileTableEntry var1) {
        synchronized(var1) {
            return var1.inode.length;
        }
    }

    int read(FileTableEntry var1, byte[] var2) {
        if (var1.mode != "w" && var1.mode != "a") {
            int var3 = 0;
            int var4 = var2.length;
            synchronized(var1) {
                while(var4 > 0 && var1.seekPtr < this.fsize(var1)) {
                    int var6 = var1.inode.findTargetBlock(var1.seekPtr);
                    if (var6 == -1) {
                        break;
                    }

                    byte[] var7 = new byte[512];
                    SysLib.rawread(var6, var7);
                    int var8 = var1.seekPtr % 512;
                    int var9 = 512 - var8;
                    int var10 = this.fsize(var1) - var1.seekPtr;
                    int var11 = Math.min(Math.min(var9, var4), var10);
                    System.arraycopy(var7, var8, var2, var3, var11);
                    var1.seekPtr += var11;
                    var3 += var11;
                    var4 -= var11;
                }

                return var3;
            }
        } else {
            return -1;
        }
    }

    int write(FileTableEntry var1, byte[] var2) {
        if (var1.mode == "r") {
            return -1;
        } else {
            synchronized(var1) {
                int var4 = 0;
                int var5 = var2.length;

                while(var5 > 0) {
                    int var6 = var1.inode.findTargetBlock(var1.seekPtr);
                    if (var6 == -1) {
                        short var7 = (short)this.superblock.getFreeBlock();
                        switch(var1.inode.registerTargetBlock(var1.seekPtr, var7)) {
                            case -3:
                                short var8 = (short)this.superblock.getFreeBlock();
                                if (!var1.inode.registerIndexBlock(var8)) {
                                    SysLib.cerr("ThreadOS: panic on write\n");
                                    return -1;
                                }

                                if (var1.inode.registerTargetBlock(var1.seekPtr, var7) != 0) {
                                    SysLib.cerr("ThreadOS: panic on write\n");
                                    return -1;
                                }
                            case 0:
                            default:
                                var6 = var7;
                                break;
                            case -2:
                            case -1:
                                SysLib.cerr("ThreadOS: filesystem panic on write\n");
                                return -1;
                        }
                    }

                    byte[] var13 = new byte[512];
                    if (SysLib.rawread(var6, var13) == -1) {
                        System.exit(2);
                    }

                    int var14 = var1.seekPtr % 512;
                    int var9 = 512 - var14;
                    int var10 = Math.min(var9, var5);
                    System.arraycopy(var2, var4, var13, var14, var10);
                    SysLib.rawwrite(var6, var13);
                    var1.seekPtr += var10;
                    var4 += var10;
                    var5 -= var10;
                    if (var1.seekPtr > var1.inode.length) {
                        var1.inode.length = var1.seekPtr;
                    }
                }

                var1.inode.toDisk(var1.iNumber);
                return var4;
            }
        }
    }

    private boolean deallocAllBlocks(FileTableEntry var1) {
        if (var1.inode.count != 1) {
            return false;
        } else {
            byte[] var2 = var1.inode.unregisterIndexBlock();
            if (var2 != null) {
                byte var3 = 0;

                short var4;
                while((var4 = SysLib.bytes2short(var2, var3)) != -1) {
                    this.superblock.returnBlock(var4);
                }
            }

            int var5 = 0;

            while(true) {
                Inode var10001 = var1.inode;
                if (var5 >= 11) {
                    var1.inode.toDisk(var1.iNumber);
                    return true;
                }

                if (var1.inode.direct[var5] != -1) {
                    this.superblock.returnBlock(var1.inode.direct[var5]);
                    var1.inode.direct[var5] = -1;
                }

                ++var5;
            }
        }
    }

    boolean delete(String var1) {
        FileTableEntry var2 = this.open(var1, "w");
        short var3 = var2.iNumber;
        return this.close(var2) && this.directory.ifree(var3);
    }

    int seek(FileTableEntry var1, int var2, int var3) {
        synchronized(var1) {
            switch(var3) {
                case 0:
                    if (var2 >= 0 && var2 <= this.fsize(var1)) {
                        var1.seekPtr = var2;
                        break;
                    }

                    return -1;
                case 1:
                    if (var1.seekPtr + var2 >= 0 && var1.seekPtr + var2 <= this.fsize(var1)) {
                        var1.seekPtr += var2;
                        break;
                    }

                    return -1;
                case 2:
                    if (this.fsize(var1) + var2 < 0 || this.fsize(var1) + var2 > this.fsize(var1)) {
                        return -1;
                    }

                    var1.seekPtr = this.fsize(var1) + var2;
            }

            return var1.seekPtr;
        }
    }
}