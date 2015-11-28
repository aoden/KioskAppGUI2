package com.tdt.kioskapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdt.kioskapp.dto.AccessTokenDTO;
import com.tdt.kioskapp.dto.KeyDTO;
import com.tdt.kioskapp.dto.RequestPackageDTO;
import com.tdt.kioskapp.dto.SlideDTO;
import com.tdt.kioskapp.model.Key;
import com.tdt.kioskapp.model.Request;
import com.tdt.kioskapp.repository.KeyRepository;
import com.tdt.kioskapp.repository.RequestRepository;
import com.tdt.kioskapp.ui.KioskUI;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class BaseService extends AbstractService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private KeyRepository keyRepository;
    @Autowired
    private RequestRepository requestRepository;
    @Value("${url}")
    private String baseURL;
    @Value("firebase.url")
    private String firebaseURL;

    public static String reformatPath(String location) {
        String reformattedPath = "";
        if (location.contains("/../")) {

            String[] fragments = location.split("/");
            for (int i = 0; i < fragments.length; i++) {
                if ((i < fragments.length - 1 && !fragments[i + 1].equals("..")) && !fragments[i].equals("..")) {
                    reformattedPath += (i == 0 ? fragments[i] : ("/" + fragments[i]));
                }
            }
            reformattedPath += ("/" + fragments[fragments.length - 1]);
            reformattedPath.replace("/../", "");
            return refactorSlashingStyle(reformattedPath);
        } else {
            return refactorSlashingStyle(location);
        }
    }

    /**
     * Because VLCJ uses native libraries, for windows it uses "\" in stead of "/" in paths so we must detect and change it
     *
     * @param reformattedPath path to be changed
     * @return changedPath
     */
    private static String refactorSlashingStyle(String reformattedPath) {
        if (SystemUtils.IS_OS_WINDOWS) {
            reformattedPath = reformattedPath.replace("/", "\\");
        }
        return reformattedPath;
    }

    public void downloadAndUnpack(String key) throws Exception {

        List<Request> requests = requestRepository.findAll();
        for (Request req : requests) {

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(baseURL + "download-package")
                    .queryParam("access_token", key)
                    .queryParam("request", req.getRequest());
            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept", "*/*");
            headers.add("Accept-Encoding", "gzip, deflate, sdch");
            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
            byte[] data = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, httpEntity, byte[].class).getBody();
            File file = new File(reformatPath(TEMP_DIR + "/data.zip"));
            FileUtils.writeByteArrayToFile(file, data);
            ZipFile zipFile = new ZipFile(file);
            zipFile.extractAll(reformatPath(TEMP_DIR));
        }
    }

    public RequestPackageDTO getRequest(String key, String manifest) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseURL + "request-package")
                .queryParam("access_token", key)
                .queryParam("manifest", manifest);
        RequestPackageDTO requestPackageDTO =
                restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, null, RequestPackageDTO.class).getBody();
        requestRepository.save(Request.builder().request(requestPackageDTO.getRequest()).build());
        return requestPackageDTO;
    }

    public int countFiles(String location) {

        File folder = new File(location);
        File[] listOfFiles = folder.listFiles();
        int result = 0;
        for (File file : listOfFiles) {
            result++;
        }
        return result;
    }

    public File findFile(String location, String fileName) {

        File folder = new File(location);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile() || file.getName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }

    /**
     * read file from a directory, this method only return 1 file
     *
     * @param location dir location
     * @return the only file inside directory
     */
    public File readAllFiles(String location) {

        File folder = new File(location);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            return null;
        } else {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    return file;
                }
            }
        }
        return null;
    }

    public Map<String, SlideDTO> readManifest(String tokenKey) throws Exception {

        return KioskUI.playCount > 1 && KioskUI.activated ? processData() : processData(getData(login(register(tokenKey))));
    }

    public Map<String, SlideDTO> readManifest() throws Exception {
        List<Key> keys = keyRepository.findAll();
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        return KioskUI.playCount > 1 && KioskUI.activated ? processData() : processData(getData(getLogin(keys)));
    }

    public String getLogin(List<Key> keys) throws Exception {
        if (keys == null) {
            keys = keyRepository.findAll();
        }
        return login(keys.get(0).getKey());
    }

    public boolean registered() {

        return keyRepository.findAll().size() != 0;
    }

    protected Map<String, SlideDTO> processData(byte[] data) throws IOException, ZipException {

////        String basePath = URLDecoder.decode(Application.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
//        File temp = new File(reformatPath(TEMP_DIR));
//        if (temp.isFile() || !temp.exists()) {
//            temp.mkdir();
//        }
        File file = new File(reformatPath(TEMP_DIR + "/data.zip"));
        FileUtils.writeByteArrayToFile(file, data);
        ZipFile zipFile = new ZipFile(file);
        zipFile.extractAll(reformatPath(TEMP_DIR));
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(reformatPath(TEMP_DIR + "/" + MANIFEST_JSON)), new TypeReference<Map<String, SlideDTO>>() {
        });
    }

    protected Map<String, SlideDTO> processData() throws IOException, ZipException {
//        String basePath = URLDecoder.decode(Application.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(reformatPath(TEMP_DIR + "/" + MANIFEST_JSON)), new TypeReference<Map<String, SlideDTO>>() {
        });
    }

    protected byte[] getData(String key) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseURL + "package")
                .queryParam("access_token", key);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "*/*");
        headers.add("Accept-Encoding", "gzip, deflate, sdch");
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(builder.build().encode().toUri(),
                HttpMethod.GET,
                httpEntity,
                byte[].class).getBody();
    }

    protected String login(String key) throws Exception {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseURL + "auth")
                .queryParam("client_id", ID)
                .queryParam("client_secret", SECRET)
                .queryParam("key", key)
                .queryParam("grant_type", "key_provider");
        AccessTokenDTO dto = restTemplate.postForEntity(builder.build().encode().toUri(), null, AccessTokenDTO.class).getBody();
        return dto.getAccessToken();
    }

    @Transactional(readOnly = false)
    public String register(String key) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseURL + "/activate/");
        builder.path(key);
        KeyDTO keyDTO = restTemplate.postForEntity(builder.build().encode().toUri(), null, KeyDTO.class).getBody();
        keyRepository.save(keyDTO.toEntity());
        return keyDTO.getKey();
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String getFirebaseURL() {
        return firebaseURL;
    }
}