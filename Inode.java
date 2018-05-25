public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    public static final int NoError = 0;
    public static final int ErrorBlockRegistered = -1;
    public static final int ErrorPrecBlockUnused = -2;
    public static final int ErrorIndirectNull = -3;

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    Inode( short iNumber ) {                       // retrieving inode from disk
        int var2 = 1 + var1 / 16;
        byte[] var3 = new byte[512];
        SysLib.rawread(var2, var3);
        int var4 = var1 % 16 * 32;
        this.length = SysLib.bytes2int(var3, var4);
        var4 += 4;
        this.count = SysLib.bytes2short(var3, var4);
        var4 += 2;
        this.flag = SysLib.bytes2short(var3, var4);
        var4 += 2;

        for(int var5 = 0; var5 < 11; ++var5) {
            this.direct[var5] = SysLib.bytes2short(var3, var4);
            var4 += 2;
        }

        this.indirect = SysLib.bytes2short(var3, var4);
        var4 += 2;
    }

    int toDisk( short iNumber ) {                  // save to disk as the i-th inode
        byte[] var2 = new byte[32];
        byte var3 = 0;
        SysLib.int2bytes(this.length, var2, var3);
        int var6 = var3 + 4;
        SysLib.short2bytes(this.count, var2, var6);
        var6 += 2;
        SysLib.short2bytes(this.flag, var2, var6);
        var6 += 2;

        int var4;
        for(var4 = 0; var4 < 11; ++var4) {
            SysLib.short2bytes(this.direct[var4], var2, var6);
            var6 += 2;
        }

        SysLib.short2bytes(this.indirect, var2, var6);
        var6 += 2;
        var4 = 1 + var1 / 16;
        byte[] var5 = new byte[512];
        SysLib.rawread(var4, var5);
        var6 = var1 % 16 * 32;
        System.arraycopy(var2, 0, var5, var6, 32);
        SysLib.rawwrite(var4, var5);
    }
}