package net.tigereye.chestcavity.compat.guzhenren.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 简易扫描工具：解析反编译的古真人物品类，提取 tooltip 中的流派信息。
 *
 * <p>默认读取 {@code ../decompile/10_6_decompile/net/guzhenren/item} 目录以及对应的 {@code
 * GuzhenrenModItems.java} 注册文件，输出“item id → 流派”映射，并标记缺失项。
 */
public final class GuzhenrenFlowScanner {
  private static final Pattern REGISTRY_ENTRY =
      Pattern.compile("REGISTRY\\.register\\(\"([^\"]+)\",\\s*(.*?)\\);", Pattern.DOTALL);
  private static final Pattern CLASS_FROM_CTOR_REF = Pattern.compile("([A-Za-z0-9_]+)::new");
  private static final Pattern CLASS_FROM_NEW = Pattern.compile("new\\s+([A-Za-z0-9_]+)");
  private static final Pattern FLOW_COMPONENT =
      Pattern.compile("Component\\.(?:literal|translatable)\\(\"([^\"]*流派[^\"]*)\"\\)");
  private static final Pattern GENERIC_LITERAL = Pattern.compile("\"([^\"]*流派[^\"]*)\"");
  private static final Pattern COLOR_CODE =
      Pattern.compile("§[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);

  private GuzhenrenFlowScanner() {}

  public record FlowEntry(String itemId, String className, List<String> flows) {
    public FlowEntry {
      flows = List.copyOf(flows);
    }

    public boolean hasFlow() {
      return !flows.isEmpty();
    }

    public String flowSummary() {
      return hasFlow() ? String.join("、", flows) : "无";
    }

    public String itemLabel() {
      return itemId != null ? itemId : className;
    }
  }

  public record ScanResult(List<FlowEntry> entries, Set<String> missingIdClasses) {
    public ScanResult {
      entries = List.copyOf(entries);
      missingIdClasses = Set.copyOf(missingIdClasses);
    }
  }

  public static void main(String[] args) throws IOException {
    Path repoRoot = Paths.get("").toAbsolutePath();
    Path itemDir = null;
    Path registryFile = null;

    try {
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        switch (arg) {
          case "--items", "-i" -> {
            ensureHasNext(args, i, arg);
            itemDir = Paths.get(args[++i]).toAbsolutePath().normalize();
          }
          case "--registry", "-r" -> {
            ensureHasNext(args, i, arg);
            registryFile = Paths.get(args[++i]).toAbsolutePath().normalize();
          }
          case "--help", "-h" -> {
            printUsage();
            return;
          }
          default -> {
            System.err.println("未知参数：" + arg);
            printUsage();
            return;
          }
        }
      }
    } catch (IllegalArgumentException ex) {
      System.err.println(ex.getMessage());
      printUsage();
      return;
    }

    if (itemDir == null) {
      itemDir = defaultItemDirectory(repoRoot);
    }
    if (registryFile == null) {
      registryFile = deriveRegistryPath(itemDir);
    }

    if (!Files.isDirectory(itemDir)) {
      System.err.println("找不到 item 目录：" + itemDir);
      return;
    }
    if (!Files.isRegularFile(registryFile)) {
      System.err.println("找不到 GuzhenrenModItems 注册文件：" + registryFile);
      return;
    }

    ScanResult result = scan(itemDir, registryFile);
    result
        .entries()
        .forEach(
            entry -> {
              System.out.printf(
                  Locale.ROOT,
                  "%-40s -> %-10s (class: %s)%n",
                  entry.itemLabel(),
                  entry.flowSummary(),
                  entry.className());
            });

    long missingFlow = result.entries().stream().filter(entry -> !entry.hasFlow()).count();
    System.out.printf(
        Locale.ROOT, "%n总计 %d 个条目，其中 %d 个缺少流派信息。%n", result.entries().size(), missingFlow);

    if (!result.missingIdClasses().isEmpty()) {
      System.err.println("\n以下类未在 GuzhenrenModItems 中找到注册项：");
      result.missingIdClasses().forEach(name -> System.err.println("  - " + name));
    }
  }

  public static ScanResult scan(Path itemDirectory, Path registryFile) throws IOException {
    Map<String, String> classToId = parseRegistry(registryFile);
    List<Path> itemFiles;
    try (Stream<Path> stream = Files.list(itemDirectory)) {
      itemFiles =
          stream
              .filter(path -> path.getFileName().toString().endsWith(".java"))
              .sorted()
              .collect(Collectors.toCollection(ArrayList::new));
    }

    List<FlowEntry> entries = new ArrayList<>();
    Set<String> missingIdClasses = new TreeSet<>();

    for (Path file : itemFiles) {
      String className = stripExtension(file.getFileName().toString());
      List<String> flows = readFlows(file);
      String itemId = classToId.get(className);
      if (itemId == null) {
        missingIdClasses.add(className);
      }

      entries.add(new FlowEntry(itemId, className, flows));
    }

    entries.sort(Comparator.comparing(FlowEntry::itemLabel));
    return new ScanResult(entries, missingIdClasses);
  }

  private static Map<String, String> parseRegistry(Path registryFile) throws IOException {
    String content = Files.readString(registryFile, StandardCharsets.UTF_8);
    Matcher matcher = REGISTRY_ENTRY.matcher(content);
    Map<String, String> result = new java.util.HashMap<>();

    while (matcher.find()) {
      String id = matcher.group(1);
      String body = matcher.group(2);
      String className = extractClassName(body);
      if (className != null && !result.containsKey(className)) {
        result.put(className, "guzhenren:" + id);
      }
    }

    return result;
  }

  private static String extractClassName(String body) {
    Matcher ctorRef = CLASS_FROM_CTOR_REF.matcher(body);
    if (ctorRef.find()) {
      return ctorRef.group(1);
    }
    Matcher newMatcher = CLASS_FROM_NEW.matcher(body);
    if (newMatcher.find()) {
      return newMatcher.group(1);
    }
    return null;
  }

  private static List<String> readFlows(Path file) {
    String content;
    try {
      content = Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("无法读取文件：" + file, e);
    }

    Set<String> flows = new LinkedHashSet<>();
    Matcher flowMatcher = FLOW_COMPONENT.matcher(content);
    while (flowMatcher.find()) {
      extractFlow(flowMatcher.group(1)).ifPresent(flows::add);
    }

    if (flows.isEmpty()) {
      Matcher fallback = GENERIC_LITERAL.matcher(content);
      while (fallback.find()) {
        extractFlow(fallback.group(1)).ifPresent(flows::add);
      }
    }

    return new ArrayList<>(flows);
  }

  private static Optional<String> extractFlow(String rawText) {
    if (rawText == null) {
      return Optional.empty();
    }

    String clean = COLOR_CODE.matcher(rawText).replaceAll("");
    clean = clean.replace("\\n", " ").replace("\\t", " ").trim();

    int marker = clean.indexOf("流派");
    if (marker < 0) {
      return Optional.empty();
    }

    int colon = indexOfColon(clean, marker + 2);
    String value = colon >= 0 ? clean.substring(colon + 1) : clean.substring(marker + 2);
    value = value.replaceAll("[\"'）】\\s]+$", "").trim();

    if (value.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  private static int indexOfColon(String text, int start) {
    int fullWidth = text.indexOf('：', start);
    int halfWidth = text.indexOf(':', start);
    if (fullWidth < 0) {
      return halfWidth;
    }
    if (halfWidth < 0) {
      return fullWidth;
    }
    return Math.min(fullWidth, halfWidth);
  }

  private static String stripExtension(String fileName) {
    int dot = fileName.lastIndexOf('.');
    return dot >= 0 ? fileName.substring(0, dot) : fileName;
  }

  private static Path defaultItemDirectory(Path repoRoot) {
    Path parent = repoRoot.getParent();
    if (parent == null) {
      parent = repoRoot;
    }
    return parent.resolve("decompile/10_6_decompile/net/guzhenren/item").normalize();
  }

  private static Path deriveRegistryPath(Path itemDir) {
    Path parent = itemDir.getParent();
    if (parent == null) {
      return itemDir.resolve("GuzhenrenModItems.java").normalize();
    }
    return parent.resolve("init/GuzhenrenModItems.java").normalize();
  }

  private static void ensureHasNext(String[] args, int index, String flag) {
    if (index + 1 >= args.length) {
      throw new IllegalArgumentException("参数缺少值：" + flag);
    }
  }

  private static void printUsage() {
    System.out.println(
        """
            用法：java -cp build/classes/java/main net.tigereye.chestcavity.compat.guzhenren.util.GuzhenrenFlowScanner [选项]
              -i, --items <path>     指定反编译的 item 目录
              -r, --registry <path>  指定 GuzhenrenModItems.java 文件
              -h, --help             显示帮助
            """);
  }
}
