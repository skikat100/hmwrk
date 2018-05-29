//A SuperBlock is the first disk block, used to describe 
//number of disk blocks, inodes, and the number of the head empty block
public class SuperBlock {
    private final int inodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

	//Constructor that takes in a int for the amount of blocks
    public SuperBlock(int diskSize)
    {
		//Read the first disk block into a buffer
        byte[] buffer = new byte[512];
        SysLib.rawread(0, buffer);

		//Initialize this SuperBlock
        totalBlocks = SysLib.bytes2int(buffer, 0);
        totalInodes = SysLib.bytes2int(buffer, 4);
        freeList = SysLib.bytes2int(buffer, 8);

		//If the current disk blocks are invalid, format it with the default size
        if (totalInodes <= 0 || totalBlocks != diskSize || freeList < 2)
        {
			SysLib.cerr("format( " + inodeBlocks + ")\n");
            totalBlocks = diskSize;
            format(inodeBlocks);
        }
    }

	//Writes the current SuperBlock into disk
    public void superSync()
    {
		//Convert fields into bytes
        byte[] buffer = new byte[512];
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList, buffer, 8);

		//Write the bytes to disk
        SysLib.rawwrite(0, buffer);
        SysLib.cerr("Superblock Synchronized\n");
    }

	//Format the disk with the maximum amount of files int maxFiles
    public void format(int maxFiles)
    {
        totalInodes = maxFiles;		//Set the amount of inodes to max files needed

		//Create the inodes and initialize to default values
        for(short i = 0; i < totalInodes; i++)
        {
            Inode inode = new Inode();
            inode.flag = 0;		//Set the inode to unused
            inode.toDisk(i);	//Save the inode to disk at the i-th node
        }
		//1 SuperBlock + (totalInodes * 32 bytes iNodeSize / 512 bytes for each block)
        freeList = 1 + (totalInodes * 32 / 512);

		//Go through each of the free blocks
        for(int i = freeList; i < totalBlocks; i++)
        {
            byte[] buffer = new byte[512];

			//Initialize the buffer array to 0 
            for(int j = 0; j < 512; ++j)
            {
                buffer[j] = 0;
            }

			//Write the buffer array into disk at position i + 1 next block
            SysLib.int2bytes(i + 1, buffer, 0);
            SysLib.rawwrite(i, buffer);
        }

		//Write the SuperBlock to disk
        superSync();
    }

	//Dequeue the top block from the free list
    public int getFreeBlock()
    {
        int block = freeList;	//Get the top block index
        if (block != -1)		//If there is an empty block
        {
			//Read in the block's index that it was pointing to
            byte[] buffer = new byte[512];
            SysLib.rawread(block, buffer);

			//Set the freeList to the next block that it was pointing to
            freeList = SysLib.bytes2int(buffer, 0);
            SysLib.int2bytes(0, buffer, 0);
            SysLib.rawwrite(block, buffer);
        }

        return block;	//Return the empty block
    }

	//Enqueue a given block to the front of the free list
    public boolean returnBlock(int blockNumber)
    {
		//Error, block invalid
        if (blockNumber < 0)
        {
            return false;
        }
        else
        {
			//Create and initialize buffer to 0
            byte[] buffer = new byte[512];

            for(int i = 0; i < 512; i++)
            {
                buffer[i] = 0;
            }
            SysLib.int2bytes(freeList, buffer, 0);
            SysLib.rawwrite(blockNumber, buffer);	//Overwrite disk to empty buffer
            freeList = blockNumber;		//Set the block to the front of the free list
            return true;
        }
    }