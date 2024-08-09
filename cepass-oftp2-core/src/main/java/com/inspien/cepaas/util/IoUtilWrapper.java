package com.inspien.cepaas.util;

import java.io.File;
import java.io.IOException;

public interface IoUtilWrapper {
    void move(File source, File destination) throws IOException;
}
