import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;
import specification.model.FileWrapper;
import specification.model.FolderResult;
import specification.model.FolderWrapper;
import specification.operations.CommonOperations;
import specification.operations.file.FileArchiveOperations;
import specification.operations.file.FileBasicOperations;
import specification.operations.folder.FolderBasicOperations;
import specification.storage.StorageOperations;

import java.io.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DropboxFile implements CommonOperations, FileBasicOperations, FolderBasicOperations,
                                FileArchiveOperations, StorageOperations
{
    private static final String ACCESS_TOKEN = "7DVVktURjwAAAAAAAAAACjDTkt7EKyq_PW7AxXAZcsdSF5It6NliI75vHKzRrHxD";
    private DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial");
    private DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
    private String root;
    private List<String> forbiddenExtensions;

    public FolderWrapper listFolder(String path, String folderName) throws Exception
    {
        List<FolderResult> folderResult = new ArrayList<>();
        if(!path.isEmpty()) path = addSlashes(path) + folderName;
        else path = addSlashes(folderName);
        path = Paths.get(root, path).toString();
        System.out.println(path);
        ListFolderResult result = client.files().listFolder(path);
        String folderMetadata = "";
        String folderMetadataName = "._" + folderName + ".txt";
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                if(folderMetadata.isEmpty() && metadata.getName().equals(folderMetadataName)) {
                    System.out.println("imam metu");
                    System.out.println(path + "/" + folderMetadataName);
                    folderMetadata = this.downloadMetadata(path + "/" + folderMetadataName);
                    continue;
                }
                if(metadata.getName().startsWith("._")) continue;
                boolean isFolder = false;
                if(metadata instanceof FolderMetadata) {
                    isFolder = true;
                }
                folderResult.add(new FolderResult(metadata.getName(), isFolder));
            }

            if (!result.getHasMore()) {
                break;
            }

            result = client.files().listFolderContinue(result.getCursor());
        }
        FolderWrapper folderWrapper = new FolderWrapper(folderResult, folderMetadata);
        return folderWrapper;
    }

    @Override
    public void uploadAsZipFile(List<File> files, String path, String zipFileName, String meta)
    {

    }

    @Override
    public void init(String pathToRoot, List<String> forbiddenExtensions) {
        this.root = addSlashes(pathToRoot);
        this.forbiddenExtensions = forbiddenExtensions;
    }

    @Override
    public void uploadFile(File file, String onPath, String fileName) throws IOException, Exception {
        if(invalidExtension(fileName)) {
            System.out.println("File contains illegal extension");
            return;
        }
        String path = addSlashes(onPath);
        path = path + fileName;
        path = Paths.get(root, path).toString();
        System.out.println(path);
        InputStream in = new FileInputStream(file);
        client.files().uploadBuilder(path).uploadAndFinish(in);
        // TODO throw FileAlreadyExistsException
    }

    @Override
    public void uploadFile(FileWrapper file) throws IOException, Exception {
        if(invalidExtension(file.getName())) {
            System.out.println("File contains illegal extension");
            return;
        }
        InputStream stream = new ByteArrayInputStream(file.getMetadata().getBytes());
        String path = getFileMetadataPath(file.getPath(), file.getName());
        path = Paths.get(root, path).toString();
        System.out.println(path);
        client.files().uploadBuilder(path).uploadAndFinish(stream);

        this.uploadFile(file.getFile(), file.getPath(), file.getName());
        // TODO delete metadata if errors
        // TODO throw FileAlreadyExistsException
    }

    @Override
    public void uploadMultipleFiles(List<FileWrapper> files) {
        CyclicBarrier barrier = new CyclicBarrier(files.size() + 1, () -> System.out.println("Prosli"));
        for (FileWrapper file : files) {
            new Thread(() -> {
                try {
                    this.uploadFile(file);
                    barrier.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException | BrokenBarrierException | IOException e) {
//                    e.printStackTrace();
                    // todo Throw exception
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }).start();
        }
        try {
            barrier.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
//            e.printStackTrace();
        } catch (BrokenBarrierException e) {
//            e.printStackTrace();
        } catch (TimeoutException e) {
//            e.printStackTrace();
        }

    }

    @Override
    public FileWrapper downloadFile(String pathToFile, String fileName, String localPath) throws IOException, Exception
    {
        String path = this.buildDbxFilePath(pathToFile, fileName);
        path = Paths.get(root, path).toString();
        DbxDownloader<FileMetadata> downloader = client.files().download(path);
        FileOutputStream out = new FileOutputStream(localPath);
        downloader.download(out);
        out.close();

        String meta = downloadMetadata(getFileMetadataPath(pathToFile, fileName));
        return new FileWrapper(new File(localPath), meta, fileName, pathToFile);
    }

    @Override
    public void createFolder(String location, String name) throws Exception {
        String path = this.buildDbxFilePath(location, name);
        path = Paths.get(root, path).toString();
        client.files().createFolderV2(path);
    }

    @Override
    public void createFolder(String location, String name, String metadata) throws Exception {
        this.createFolder(location, name);
        System.out.println(this.getFolderMetadataPath(location, name));
        InputStream stream = new ByteArrayInputStream(metadata.getBytes());
        String path = this.getFolderMetadataPath(location, name);
        path = Paths.get(root, path).toString();
        client.files().uploadBuilder(path).uploadAndFinish(stream);
    }

    @Override
    public void delete(String path, String name) throws Exception {

    }

    private String downloadMetadata(String path) throws IOException {
        path = Paths.get(root, path).toString();
        String meta = "";
        try {
            DbxDownloader<FileMetadata> metaDownloader = client.files().download(path);
            meta = this.inputStreamToString(metaDownloader.getInputStream());
        } catch (DownloadErrorException e) {
            // TODO check if it is path : not_found
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return meta;
    }

    private String buildDbxFilePath(String path, String name) {
        path = this.addSlashes(path);
        String dbxPath = path + name;
        return dbxPath;
    }

    private String getFileMetadataPath(String path, String name) {
        path = this.addSlashes(path);
        return path + ".__" + name + ".txt";
    }

    private String getFolderMetadataPath(String path, String name) {
        path = this.addSlashes(path);
        return path + name + "/._" + name + ".txt";
    }

    private String addSlashes(String path) {
        if(path.isEmpty()) return path;
        if(!path.startsWith("/")) path = "/" + path;
        if(!path.endsWith("/")) path = path + "/";
        return path;
    }

    private boolean invalidExtension(String fileName)
    {
        return this.forbiddenExtensions.contains(fileName.substring(fileName.lastIndexOf(".")+ 1));
    }


    public String inputStreamToString(InputStream inputStream) throws IOException {
        try(ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString();
        }
    }
}
