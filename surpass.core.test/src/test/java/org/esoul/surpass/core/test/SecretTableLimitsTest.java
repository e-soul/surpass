package org.esoul.surpass.core.test;

import org.esoul.surpass.core.SquareMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SecretTableLimitsTest {

    private byte[][] bytes;
    private SquareMatrix dt;

    @BeforeEach
    public void setUp() {
        dt = new SquareMatrix();
        bytes = dt.getBytes();
    }

    @Test
    public void testMaxRow() throws Exception {
        
//        for (int i = 0; i < 256; ++i) {
//            System.out.println(i);
//            dt.createRow("dummy".toCharArray(), "dummy".toCharArray(), "dummy".toCharArray());
//        }
        

//        byte a[] = new byte[] {-1};
//        int i = Byte.toUnsignedInt(a[0]);
//        System.out.println(i);
//        System.out.println(Integer.compareUnsigned(i, 0));
        
        byte a = -1;
        int b = 257;
        System.out.println((byte)b);
        System.out.println(a == (byte) b);
    }
}
