public class Directory 
{
	private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.
	
	// directory constructor
    public Directory( int maxInumber ) 
	{
        fsize = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

	// assumes data[] received directory information from disk
	// initializes the Directory instance with this data[]
    public void bytes2directory( byte data[] ) 
	{
        int offset = 0;

		// go through each file and set the file size at i to the info given in data
        for(int i = 0; i < fsize.length; offset += 4, i++) 
		{
            fsize[i] = SysLib.bytes2int(data, offset);
        }

		// go through each of the file names 
        for(int i = 0; i < fnames.length; offset += maxChars * 2, i++) 
		{
            String name = new String(data, offset, maxChars * 2);
            name.getChars(0, fsize[i], fnames[i], 0);	// initialize the chars in data to name
        }
    }

	// converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted into bytes.	
    public byte[] directory2bytes( ) 
	{
        byte[] buffer = new byte[(4 * fsize.length) + (2 * fnames.length * maxChars)];
        int offset = 0;

		// goes through each file, turns them into bytes, and reads into the buffer
        for (int i = 0; i < fsize.length; offset += 4, i++) 
		{
            SysLib.int2bytes( fsize[i], buffer, offset );
        }

		// goes through each file name, turns them into bytes, and reads into the buffer
        for (int i = 0; i < fnames.length; offset += maxChars * 2, i++) 
		{
            String name = new String(fnames[i], 0, fsize[i]);
            byte[] stringBuffer = name.getBytes();
            System.arraycopy(stringBuffer, 0, buffer, offset, stringBuffer.length);
        }

        return buffer;	
    }

	// filename is the one of a file to be created.
	// allocates a new inode number for this filename
    public short ialloc( String filename ) 
	{
		// for each file index
        for (short i = 1; i < fsize.length; i++) 
		{
            if (fsize[i] == 0) // if there is an empty slot 
			{
                fsize[i] = Math.min(maxChars, filename.length()); // set the the size of file
                filename.getChars(0, fsize[i], fnames[i], 0); // set the filename 
                return i;
            }
        }
        return -1;
    }

    // deallocates this inumber (inode number)
	// the corresponding file will be deleted.
    public boolean ifree( short iNumber ) 
	{
        if (fsize[iNumber] > 0) 
		{
            fsize[iNumber] = 0;	// deallocated
            return true;
        } 
		else 
		{
            return false;
        }
    }

	// returns the inumber corresponding to this filename
    public short namei( String filename ) 
	{
		// go through all of the files
        for (short i = 0; i < fsize.length; i++) 
		{
            if (fsize[i] == filename.length( )) // if the length of the strings match
			{
                String name = new String(fnames[i], 0, fsize[i]); // create the string

                if (filename.compareTo(name) == 0) // compare strings
				{
                    return i; // return the corresponding inumber
                }
            }
        }
        return -1;
    }
}