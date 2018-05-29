public class Inode 
{
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
	public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    // a default constructor
	Inode( )                                       
    {
		length = 0;
		count = 0;
		flag = 1;
		for ( int i = 0; i < directSize; i++ )
			direct[i] = -1;
		indirect = -1;
    }

    // retrieving inode from disk
    Inode( short iNumber )                           
    {   
		// 1 Superblock + iNumber posiiton / 16 inodes in one block
		int inodeBlock = 1 + iNumber / 16;		    // get the position of the block

		// read into the buffer the inodeBlock contents 
        byte[] buffer = new byte[512];
        SysLib.rawread(inodeBlock, buffer);

		// get the inode position by iNumber % 16 inodes in a block and * 32 bytes in inode size
        int offset = iNumber % 16 * 32;

		// read in the length in Integer
        length = SysLib.bytes2int(buffer, offset);	
        offset += 4;		// ints are 4 bytes in size

		// read in the count in short
        count = SysLib.bytes2short(buffer, offset);
		offset += 2;		// shorts are 2 bytes in size

		// read in the flag in short
        flag = SysLib.bytes2short(buffer, offset);
        offset += 2;		// shorts are 2 bytes in size

		// go through all of the direct pointers
        for(short i = 0; i < 11; i++) 
		{
            direct[i] = SysLib.bytes2short(buffer, offset);
            offset += 2;	// increment by the size of a short
        }
		// the 12th pointer is indirect
        indirect = SysLib.bytes2short(buffer, offset);	// set it as indirect
        offset += 2;	//set offset position to end
    }

    // save to disk as the i-th inode
	int toDisk( short iNumber )                    
    {  
		byte[] buffer = new byte[32];

		// read into the buffer the length
        SysLib.int2bytes(length, buffer, 0);
        int offset = 4;	// int is 4 bytes
		
		// read into the buffer the count
        SysLib.short2bytes(count, buffer, offset);
        offset += 2;	// short is 2 bytes

		// read into the buffer the flag
        SysLib.short2bytes(flag, buffer, offset);
        offset += 2;	// short is 2 bytes

		// go through the direct pointers
        for(int i = 0; i < 11; i++) 
		{
			// read into buffer the direct pointers
            SysLib.short2bytes(direct[i], buffer, offset);
            offset += 2;	// short is 2 bytes
        }

		// indirect pointer is the 12th
		int pointer = 12;
        SysLib.short2bytes(indirect, buffer, offset);
        offset += 2;		// short 2 bytes

		// find block containing i-th number inode in disk 
        pointer = 1 + iNumber / 16;		// Superblock + iNumber / 16 inodes in one block
        byte[] blockBuffer = new byte[512];
        SysLib.rawread(pointer, blockBuffer);	// read existing nodes into buffer
        
		// offset to find the i-th number inode in block
		offset = iNumber % 16 * 32;

		// copy this inode data into the block buffer containing inodes
        System.arraycopy(buffer, 0, blockBuffer, offset, 32);
        SysLib.rawwrite(pointer, blockBuffer);	// write to disk. 
    }

	short findTargetBlock( int offset )
	{
		// find the block of the target
		int position = offset / 512;
        if (indirect < 0)
		{
            return -1;	// error	
        } 
		else if (position < 11) // resides in the direct blocks
		{
            return direct[position];
        } 
		else 
		{
            byte[] buffer = new byte[512];
            SysLib.rawread(indirect, buffer);	// read in the position of the indirect block
            
			// find the position of the block within the indirect buffer
			int indirectPos = position - 11;
            return SysLib.bytes2short(buffer, indirectPos * 2);		// return the target block
        }
	}

	// returns the indexBlockNumber
	public short getIndexBlockNumber( )
	{
		return indirect; 
	}

	// set the indirect block to indexBlockNumber
	boolean setIndexBlock( short indexBlockNumber )
	{
		// search through the direct pointers
        for (int i = 0; i < 11; ++i) 
		{
            if (this.direct[i] == -1) // if a direct pointer is empty/invalid
			{
                return false;
            }
        }

        if (this.indirect != -1) // if the indirect pointer is empty/invalid
		{
            return false;
        } 
		else 
		{
            indirect = indexBlockNumber;	// set the indirect pointer
            byte[] buffer = new byte[512];

			// indirect pointer points to 256 indexes block
            for(int i = 0; i < 256; i++) 
			{
                SysLib.short2bytes( (short) -1, buffer, i * 2 ); // invalidate current indirect
            }

			// write the buffer to disk
            SysLib.rawwrite(indexBlockNumber, buffer);
            return true;
        }
	}

	// Unregisters the indexBlock and returns a buffer of pointers (indirect)
	byte[] unsetIndexBlock( )
	{
		if (indirect != -1)
		{
			byte[] buffer = new byte[512];
			SysLib.rawread(indirect, buffer);	// read into buffer the prev indirect pointer
			indirect = -1;						// invalidate the indirect pointer
			return buffer;
		}
		else
		{
			return null;	// already unset
		}
	}

	// Sets the target block to an emptyBlock
	// Returns -3 if the indirect pointer is invalid
	// Returns -2 if the block preceding the current block is unused
	// Returns -1 if the block is already set
	// Returns 0 if there are no errors
	int setTargetBlock( int offset, short emptyBlock )
	{
		// get the block position within the inode
        int position = offset / 512;

		// block position is in the direct blocks
        if (position < 11) 
		{
            if (direct[position] >= 0) 
			{
                return -1;	// The block is already set
            } 
			else if (position > 0 && this.direct[position - 1] == -1) 
			{
                return -2;	// The block preceding the current block is unused
            } 
			else 
			{
                this.direct[position] = emptyBlock;	// set the direct pointer to empty block
                return 0;	// no errors noted so far
            }
        } 
		else if (indirect == -1) // Indirect pointer is invalid
		{
            return -3;
        } 
		else // position is in the indirect pointers
		{
			// read into the buffer the indirect pointers
            byte[] buffer = new byte[512];
            SysLib.rawread(indirect, buffer);

			// get the offset of the indirect pointers
            int indirectOffset = position - 11;
            if ( SysLib.bytes2short(buffer, offset * 2) > 0 ) // if the indirect block is already set
			{
                SysLib.cerr("indirect number found: " + offset + " values: " 
					+ SysLib.bytes2short(buffer, offset * 2) + "\n");
                return -1;	// Block already set
            } 
			else 
			{
				// read the empty pointer into the buffer and write to disk
                SysLib.short2bytes(emptyBlock, buffer, offset * 2);
                SysLib.rawwrite(indirect, buffer);
                return 0;	// No erors
            }
        }
	}
}