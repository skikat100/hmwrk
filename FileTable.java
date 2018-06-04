import java.util.*;

public class FileTable {

    private Vector table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods

	// allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
	// return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc( String filename, String mode ) 
	{
		int iNumber = -1;
        Inode inode = null;

        while(true)
		{
           iNumber = (filename.equals("/") ? 0 : dir.namei(filename));
           // Create the file if it doesn't exist, except in read mode
           if(iNumber == -1) {
               if(mode == "r") {
                   return null;
               }

               iNumber = dir.ialloc(filename);
           }

           if(iNumber >= 0)
		   {
              inode = new Inode ((short)iNumber);
              if(mode.equals("r"))
			  {
				 // inode.flag is read
                 if(inode.flag == 2 || inode.flag == 0) break; // No need to wait
                 else if(inode.flag == 1)  // Wait for write to finish
				 {
                    try
					{
						wait();
					} catch (InterruptedException e){}
				 }
				 // no more open
                 else if(inode.flag == -1)  // File is to be deleted
				 {
                    iNumber = -1; // No more open
                    return null;
                 }
              }
              // wait until inode is free to write if writing
              else if (mode.equals("w") || mode.equals ("w+") || mode.equals("a") )
			  { 
                 if(inode.flag == 0 || inode.flag == 2)
				 {
                    break;
                 }
                 else if(inode.flag == 1)
				 {
                    try
					{
						wait();
					} catch (InterruptedException e){}
				 }
                 else if (inode.flag == -1)
				 {
                    iNumber = -1;
                    return null;
                 }
              }
           }
        }

        if(mode.equals("a") || mode.equals("w") || mode.equals("w+") )
		{
           inode.flag = 1;
		}
        else
		{
           inode.flag = 2;
        }
		inode.count++;
        inode.toDisk( (short) iNumber );	// Save iNode to disk after every update
        FileTableEntry e = new FileTableEntry(inode, (short)iNumber, mode, filename);
        table.add(e);

		return e;
    }

    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
	// return true if this file table entry found in my table
    public synchronized boolean ffree( FileTableEntry e ) 
	{
		if (e == null)
		{
			return false;	// invalid
		}
        int index = e.iNumber;
        if (index == -1)
		{
			return false;
		}
        e.inode.flag = 0;	// free
        e.count--;
        e.inode.toDisk((short)index);	// save corresponding inode

        if (e.count == 0)  // file is not being accessed
		{
			notifyAll();
            table.remove(e);	// delete the table entry
		}
		return true;
	}

	// should be called before starting a format
    public synchronized boolean fempty( ) 
	{
        return table.isEmpty( );  // return if table is empty
    }  
    
    public synchronized boolean isFileOpen(String filename) {
        for(int i = 0; i < table.size(); i++) {
            if(((FileTableEntry)table.get(i)).fileName == filename) {
                return true;
            }
        }

        return false;
    }
}