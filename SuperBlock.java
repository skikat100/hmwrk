public class SuperBlock {
    private final int inodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public SuperBlock(int diskSize)
    {
        byte[] buffer = new byte[512];
        SysLib.rawread(0, buffer);
        totalBlocks = SysLib.bytes2int(buffer, 0);
        totalInodes = SysLib.bytes2int(buffer, 4);
        freeList = SysLib.bytes2int(buffer, 8);
        if (totalBlocks != diskSize || totalInodes <= 0 || freeList < 2)
        {
            totalBlocks = diskSize;
            SysLib.cerr("default format( " + inodeBlocks + ")\n");
            format();
        }
    }

    public void superSync()
    {
        byte[] buffer = new byte[512];
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList, buffer, 8);
        SysLib.rawwrite(0, buffer);
        SysLib.cerr("Synchronized Superblock\n");
    }

    public void format()
    {
        format(inodeBlocks);
    }

    public void format(int maxFiles)
    {
        totalInodes = maxFiles;

        for(short i = 0; i < totalInodes; ++i)
        {
            Inode iNode = new Inode();
            iNode.flag = 0;
            iNode.toDisk(i);
        }
        freeList = 2 + totalInodes * 32 / 512;

        for(int i = freeList; i < totalBlocks; ++i)
        {
            byte[] buffer = new byte[512];

            for(int j = 0; j < 512; ++j)
            {
                buffer[j] = 0;
            }

            SysLib.int2bytes(i + 1, buffer, 0);
            SysLib.rawwrite(i, buffer);
        }

        superSync();
    }

    public int getFreeBlock()
    {
        int var1 = freeList;
        if (var1 != -1)
        {
            byte[] var2 = new byte[512];
            SysLib.rawread(var1, var2);
            freeList = SysLib.bytes2int(var2, 0);
            SysLib.int2bytes(0, var2, 0);
            SysLib.rawwrite(var1, var2);
        }

        return var1;
    }

    public boolean returnBlock(int var1)
    {
        if (var1 < 0)
        {
            return false;
        }
        else
        {
            byte[] var2 = new byte[512];

            for(int var3 = 0; var3 < 512; ++var3)
            {
                var2[var3] = 0;
            }

            SysLib.int2bytes(freeList, var2, 0);
            SysLib.rawwrite(var1, var2);
            freeList = var1;
            return true;
        }
    }