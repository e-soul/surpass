package org.esoul.surpass.persist.test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveDeleteFileVisitor<T extends Path> extends SimpleFileVisitor<T> {

    @Override
    public FileVisitResult visitFile(T file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(T dir, IOException e) throws IOException {
        if (null != e) {
            throw e;
        }
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
    }
}
