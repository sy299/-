package com.community.mall.upload;

import com.community.mall.common.ApiResponse;
import com.community.mall.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
public class ImageUploadController {
  private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
  private final Path uploadRoot;

  public ImageUploadController(@Value("${mall.upload-dir:./uploads}") String uploadDir) {
    this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
  }

  @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<?> upload(@RequestPart("file") MultipartFile file) {
    if (file == null || file.isEmpty()) throw new BusinessException("请选择要上传的图片");
    if (file.getSize() > MAX_IMAGE_SIZE) throw new BusinessException("图片不能超过 5MB");

    String extension = extensionFor(file.getContentType());
    if (extension == null) throw new BusinessException("仅支持 JPG、PNG、GIF、WEBP 图片");

    try {
      Files.createDirectories(uploadRoot);
      String filename = UUID.randomUUID().toString().replace("-", "") + extension;
      Path target = uploadRoot.resolve(filename).normalize();
      if (!target.startsWith(uploadRoot)) throw new BusinessException("图片保存路径无效");
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      return ApiResponse.ok(Map.of("url", "/uploads/" + filename, "name", filename));
    } catch (IOException e) {
      throw new BusinessException("图片保存失败，请稍后重试");
    }
  }

  private static String extensionFor(String contentType) {
    if (contentType == null) return null;
    return switch (contentType.toLowerCase(Locale.ROOT)) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/gif" -> ".gif";
      case "image/webp" -> ".webp";
      default -> null;
    };
  }
}
