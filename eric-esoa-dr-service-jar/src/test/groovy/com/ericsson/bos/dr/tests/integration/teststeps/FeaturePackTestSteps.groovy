/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.bos.dr.tests.integration.teststeps

import com.ericsson.bos.dr.tests.integration.utils.JsonUtils
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackDto
import com.ericsson.bos.dr.web.v1.api.model.FeaturePackListDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import static com.ericsson.bos.dr.tests.integration.utils.IOUtils.readClasspathResource
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@Component
class FeaturePackTestSteps {

    private static final String ZIP_OUTPUT_DIR = System.getProperty("java.io.tmpdir")
    private static final String FP_URL = "/discovery-and-reconciliation/v1/feature-packs"

    @Autowired
    private MockMvc mockMvc

    FeaturePackDto uploadFeaturePack(String path, String name) {
        final AtomicReference<FeaturePackDto> jsonResponse = new AtomicReference()
        uploadFeaturePackResult(path, name)
                .andExpect(status().isCreated())
                .andDo(result -> jsonResponse.set(JsonUtils.read(result.getResponse().getContentAsString(), FeaturePackDto.class)))
        return jsonResponse.get()
    }

    ResultActions uploadFeaturePackResult(String path, String name) {
        final String archivePath = createFeaturePackArchive(path)
        final MockMultipartFile file = new MockMultipartFile("file", "hello.txt",
                MediaType.TEXT_PLAIN_VALUE, Files.readAllBytes(Paths.get(archivePath)))
        mockMvc.perform(multipart(FP_URL)
                .file(file)
                .queryParam("name", name)
                .queryParam("description", "test feature pack")
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    ResultActions uploadFeaturePackResultToken(String path, String name, String accessToken) {
        final String archivePath = createFeaturePackArchive(path)
        final MockMultipartFile file = new MockMultipartFile("file", "hello.txt",
                MediaType.TEXT_PLAIN_VALUE, Files.readAllBytes(Paths.get(archivePath)))
        mockMvc.perform(multipart(FP_URL)
                .file(file)
                .queryParam("name", name)
                .queryParam("description", "test feature pack")
                .header(HttpHeaders.AUTHORIZATION, accessToken))
    }

    ResultActions uploadFeaturePackResult(String name, byte[] archive) {
        final MockMultipartFile file = new MockMultipartFile("file", "hello.txt",
                MediaType.TEXT_PLAIN_VALUE, archive)
        mockMvc.perform(multipart(FP_URL)
                .file(file)
                .queryParam("name", name)
                .queryParam("description", "test feature pack")
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    FeaturePackDto getFeaturePack(String id) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        getFeaturePackResult(id)
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), FeaturePackDto.class)))
        return response.get()

    }

    ResultActions getFeaturePackResult(String id) {
        return mockMvc.perform(get("${FP_URL}/${id}")
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    FeaturePackListDto getFeaturePacks() {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        mockMvc.perform(get(FP_URL))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), FeaturePackListDto.class)))
        return response.get()
    }

    FeaturePackListDto getFeaturePacksWithPagination(String pageRequest) {
        final AtomicReference<FeaturePackDto> response = new AtomicReference()
        mockMvc.perform(get(FP_URL + pageRequest))
                .andExpect(status().isOk())
                .andDo(result -> response.set(JsonUtils.read(result.getResponse().getContentAsString(), FeaturePackListDto.class)))
        return response.get()
    }

    ResultActions getFeaturePacksWithPaginationResult(String pageRequest) {
        return mockMvc.perform(get("${FP_URL}${pageRequest}"))
    }

    void deleteFeaturePack(String id) {
        deleteFeaturePackResult(id)
                .andExpect(status().isNoContent())
    }

    ResultActions deleteFeaturePackResult(String id) {
        return mockMvc.perform(delete("${FP_URL}/${id}")
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    FeaturePackDto replaceFeaturePack(String path, String id) {
        final AtomicReference<FeaturePackDto> jsonResponse = new AtomicReference()
        replaceFeaturePackResult(path, id)
                .andExpect(status().isCreated())
                .andDo(result -> jsonResponse.set(JsonUtils.read(result.getResponse().getContentAsString(), FeaturePackDto.class)))
        return jsonResponse.get()
    }

    ResultActions replaceFeaturePackResult(String path, String id) {
        final String archivePath = createFeaturePackArchive(path)
        final MockMultipartFile file = new MockMultipartFile("file", "hello.txt",
                MediaType.TEXT_PLAIN_VALUE, Files.readAllBytes(Paths.get(archivePath)))
        return mockMvc.perform(multipart(HttpMethod.PUT, "${FP_URL}/${id}")
                .file(file)
                .queryParam("description", "updated test feature pack")
                .header(HttpHeaders.AUTHORIZATION, readClasspathResource("/tokens/admin_token.json")))
    }

    byte[] downloadFeaturePack(String id) {
        final AtomicReference<byte[]> response = new AtomicReference()
        downloadFeaturePackResult(id)
                .andExpect(status().isOk())
                .andDo(result -> response.set(result.getResponse().getContentAsByteArray()))
        return response.get()
    }

    ResultActions downloadFeaturePackResult(String id) {
        final AtomicReference<byte[]> response = new AtomicReference()
        return mockMvc.perform(get("${FP_URL}/${id}/files"))
    }

    private String createFeaturePackArchive(final String path) {
        try {
            final URL baseUrl = this.getClass().getResource(path)
            final Path basePath = Paths.get(baseUrl.toURI())
            final String zipOutputPath = ZIP_OUTPUT_DIR + File.separator + basePath.getFileName().toString() + ".zip"
            final FileOutputStream fos = new FileOutputStream(zipOutputPath)
            final ZipOutputStream zos = new ZipOutputStream(fos)
            zos.putNextEntry(new ZipEntry("applications/"))
            zos.putNextEntry(new ZipEntry("listeners/"))
            zos.putNextEntry(new ZipEntry("properties/"))
            zos.putNextEntry(new ZipEntry("job_inputs/"))
            zos.putNextEntry(new ZipEntry("asserts/"))
            Files.walkFileTree(basePath, new ConfigFileVisitor(zos))
            zos.close()
            fos.close()
            return zipOutputPath
        } catch (Exception e) {
            throw new RuntimeException(e)
        }
    }

    private class ConfigFileVisitor extends SimpleFileVisitor<Path> {

        private final ZipOutputStream zos

        ConfigFileVisitor(ZipOutputStream zos) {
            this.zos = zos
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
            final File file = path.toFile()
            if (file.getName().startsWith("app")) {
                writeFileToZip(file, "applications")
            } else if (file.getName().startsWith("inputs")) {
                writeFileToZip(file, "job_inputs")
            } else if (file.getName().contains("asset")) {
                writeFileToZip(file, "assets")
            } else if (file.getName().startsWith("listener")) {
                writeFileToZip(file, "listeners")
            } else if (file.getName().startsWith("properties")) {
                writeFileToZip(file, "properties")
            }
            return FileVisitResult.CONTINUE
        }

        private void writeFileToZip(final File file, final String dir) throws IOException {
            FileInputStream fis = new FileInputStream(file)
            ZipEntry zipEntry = new ZipEntry(dir + "/" + file.getName())
            zos.putNextEntry(zipEntry)
            byte[] bytes = new byte[1024]
            int length
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length)
            }
            fis.close()
        }
    }
}