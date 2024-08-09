package com.inspien.cepaas.util;

import org.neociclo.odetteftp.util.IoUtil;
import java.io.File;
import java.io.IOException;

public class IoUtilWrapperImpl implements IoUtilWrapper {
    @Override
    public void move(File source, File destination) throws IOException {
        IoUtil.move(source, destination);
    }
}
