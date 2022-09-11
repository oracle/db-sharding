/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

package oracle.sharding.details;

/**
 * Oracle Hash Function Implementation consistent with ORA_HASH
 */
public class OracleKggHash {

    public static int hash(byte [] buffer)
    {
        return hash(buffer, 0, buffer.length, 0);
    }

    public static int hash(byte [] buffer, int base)
    {
        return hash(buffer, 0, buffer.length, base);
    }

    public static int hash(byte [] k, int start, int end, int base)
    {
        int fullLength = end - start;
        int len = fullLength;
        int i   = start;

        int a, b, c = b = a = 0x9e3779b9;
        int d = base;

        while (len >= 0)
        {
            int xa = 0, xb = 0, xc = 0, xd = 0;
            int split0 = (len & 0xfffffff0);
            int split1 = (split0 != 0) ?  0x10 : (len & 0x0000000f);

            switch (split1) {
                case 16 : xd |= ((((int) k[i+15]) & 0xff) << 24);
                case 15 : xd |= ((((int) k[i+14]) & 0xff) << 16);
                case 14 : xd |= ((((int) k[i+13]) & 0xff) << 8);
                case 13 : xd |= ((((int) k[i+12]) & 0xff));
                case 12 : xc |= ((((int) k[i+11]) & 0xff) << 24);
                case 11 : xc |= ((((int) k[i+10]) & 0xff) << 16);
                case 10 : xc |= ((((int) k[i+ 9]) & 0xff) << 8);
                case  9 : xc |= ((((int) k[i+ 8]) & 0xff));
                case  8 : xb |= ((((int) k[i+ 7]) & 0xff) << 24);
                case  7 : xb |= ((((int) k[i+ 6]) & 0xff) << 16);
                case  6 : xb |= ((((int) k[i+ 5]) & 0xff) << 8);
                case  5 : xb |= ((((int) k[i+ 4]) & 0xff));
                case  4 : xa |= ((((int) k[i+ 3]) & 0xff) << 24);
                case  3 : xa |= ((((int) k[i+ 2]) & 0xff) << 16);
                case  2 : xa |= ((((int) k[i+ 1]) & 0xff) << 8);
                case  1 : xa |= ((((int) k[i+ 0]) & 0xff));
            }

            d += (split0 == 0) ? (fullLength + (xd << 8)) : xd;

            a += xa;
            b += xb;
            c += xc;

            a += d; d += a; a ^= (a >>> 7);
            b += a; a += b; b ^= (b <<  13);
            c += b; b += c; c ^= (c >>> 17);
            d += c; c += d; d ^= (d <<  9);
            a += d; d += a; a ^= (a >>> 3);
            b += a; a += b; b ^= (b <<  7);
            c += b; b += c; c ^= (c >>> 15);
            d += c; c += d; d ^= (d <<  11);

            i += 16; len -= 16;
        }

        return d;
    }
}
