public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) { // directory constructor
        fsize = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    public void bytes2directory( byte data[] ) {
        int var2 = 0;

        int var3;
        for(var3 = 0; var3 < this.fsize.length; var2 += 4) {
            this.fsize[var3] = SysLib.bytes2int(var1, var2);
            ++var3;
        }

        for(var3 = 0; var3 < this.fnames.length; var2 += maxChars * 2) {
            String var4 = new String(var1, var2, maxChars * 2);
            var4.getChars(0, this.fsize[var3], this.fnames[var3], 0);
            ++var3;
        }
    }

    public byte[] directory2bytes( ) {
        byte[] var1 = new byte[this.fsize.length * 4 + this.fnames.length * maxChars * 2];
        int var2 = 0;

        int var3;
        for(var3 = 0; var3 < this.fsize.length; var2 += 4) {
            SysLib.int2bytes(this.fsize[var3], var1, var2);
            ++var3;
        }

        for(var3 = 0; var3 < this.fnames.length; var2 += maxChars * 2) {
            String var4 = new String(this.fnames[var3], 0, this.fsize[var3]);
            byte[] var5 = var4.getBytes();
            System.arraycopy(var5, 0, var1, var2, var5.length);
            ++var3;
        }

        return var1;
    }

    public short ialloc( String filename ) {
        for(short var2 = 1; var2 < this.fsize.length; ++var2) {
            if (this.fsize[var2] == 0) {
                this.fsize[var2] = Math.min(var1.length(), maxChars);
                var1.getChars(0, this.fsize[var2], this.fnames[var2], 0);
                return var2;
            }
        }

        return -1;
    }

    public boolean ifree( short iNumber ) {
        if (this.fsize[var1] > 0) {
            this.fsize[var1] = 0;
            return true;
        } else {
            return false;
        }
    }

    public short namei( String filename ) {
        for(short var2 = 0; var2 < this.fsize.length; ++var2) {
            if (this.fsize[var2] == var1.length()) {
                String var3 = new String(this.fnames[var2], 0, this.fsize[var2]);
                if (var1.compareTo(var3) == 0) {
                    return var2;
                }
            }
        }

        return -1;
    }
}