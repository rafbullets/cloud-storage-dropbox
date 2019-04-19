import specification.model.FileWrapper;
import specification.model.FolderResult;
import specification.model.FolderWrapper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {

        DropboxFile d = new DropboxFile();
//        d.createFolder("noviFolder1/multi1", "folderce", "{upload: 'noviFolder'}");
//        d.createFolder("/foldercic", "foldercici");
//        d.uploadFile(new File("output43.csv"), "/noviFolder", "output43.csv" );
//        d.uploadFile(new FileWrapper(new File("output.csv"),
//                "Tokilo car .csv", "out.csv", "/noviFolder1"));
        List<FileWrapper> files = new ArrayList<>();
        files.add(new FileWrapper(new File("output1.csv"), "output1", "output1.csv", "noviFolder1/multi1/folderce"));
        files.add(new FileWrapper(new File("output.csv"), "output", "output.csv", "noviFolder1/multi1/folderce"));
        files.add(new FileWrapper(new File("output43.csv"), "output43", "output43.csv", "noviFolder1/multi1/folderce"));
        d.uploadMultipleFiles(files);
//        FileWrapper f = d.downloadFile("/noviFolder/multi/", "output.csv", "outputD.csv");
//        System.out.println(f.getMetadata());

        FolderWrapper l = d.listFolder("noviFolder1/multi1/", "folderce");
        System.out.println("Meta: " + l.getMetadata());
        for (FolderResult a: l.getFolderResults()) {
            System.out.println(a);

        }
//        System.out.println(f.getMetadata());
//        System.out.println(f.getMetadata());
//        d.createFolder("/fol61/f43", "metaa43");
    }
}
