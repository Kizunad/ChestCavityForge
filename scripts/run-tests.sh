#!/bin/bash
# 测试运行脚本 - 提供多种测试运行方式

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[⚠]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

# 帮助信息
show_help() {
    cat << EOF
ChestCavity 测试运行脚本

用法: $0 [选项]

选项:
    all             运行所有测试（默认）
    unit            仅运行单元测试
    integration     仅运行集成测试
    benchmark       仅运行性能基准测试
    coverage        运行测试并生成覆盖率报告
    watch           持续监听模式（文件改动时自动运行）
    quick           快速检查（仅编译 + 静态分析）
    report          生成完整测试报告

示例:
    $0                  # 运行所有测试
    $0 unit            # 仅运行单元测试
    $0 coverage        # 生成覆盖率报告

EOF
}

# 运行所有测试
run_all_tests() {
    print_info "运行所有测试..."
    ./gradlew test --console=rich

    if [ $? -eq 0 ]; then
        print_success "所有测试通过！"
    else
        print_error "测试失败！"
        exit 1
    fi
}

# 运行单元测试
run_unit_tests() {
    print_info "运行单元测试..."
    ./gradlew test --tests "*Test" --console=rich

    if [ $? -eq 0 ]; then
        print_success "单元测试通过！"
    else
        print_error "单元测试失败！"
        exit 1
    fi
}

# 运行集成测试
run_integration_tests() {
    print_info "运行集成测试..."
    ./gradlew test --tests "*IntegrationTest" --console=rich

    if [ $? -eq 0 ]; then
        print_success "集成测试通过！"
    else
        print_error "集成测试失败！"
        exit 1
    fi
}

# 运行性能基准测试
run_benchmark_tests() {
    print_info "运行性能基准测试..."
    print_warning "这可能需要几分钟..."

    ./gradlew test --tests "*Benchmark*" --console=rich

    if [ $? -eq 0 ]; then
        print_success "基准测试完成！"
        print_info "查看输出中的性能数据"
    else
        print_error "基准测试失败！"
        exit 1
    fi
}

# 生成覆盖率报告
run_coverage() {
    print_info "运行测试并生成覆盖率报告..."

    ./gradlew test jacocoTestReport --console=rich

    if [ $? -eq 0 ]; then
        print_success "覆盖率报告生成完成！"
        print_info "报告位置: build/reports/jacoco/test/html/index.html"

        # 尝试自动打开报告
        if command -v xdg-open &> /dev/null; then
            xdg-open build/reports/jacoco/test/html/index.html 2>/dev/null || true
        fi
    else
        print_error "覆盖率生成失败！"
        exit 1
    fi
}

# 持续监听模式
run_watch_mode() {
    print_info "启动持续监听模式..."
    print_warning "文件改动时将自动运行测试"
    print_info "按 Ctrl+C 退出"

    ./gradlew test --continuous --console=rich
}

# 快速检查（仅编译 + 静态分析）
run_quick_check() {
    print_info "快速检查模式..."

    print_info "1/3 编译检查..."
    ./gradlew compileJava compileTestJava --console=plain

    if [ $? -ne 0 ]; then
        print_error "编译失败！"
        exit 1
    fi
    print_success "编译通过"

    print_info "2/3 Checkstyle 检查..."
    ./gradlew checkstyleMain --console=plain

    if [ $? -ne 0 ]; then
        print_warning "Checkstyle 发现问题（仅警告）"
    else
        print_success "Checkstyle 通过"
    fi

    print_info "3/3 快速单元测试..."
    ./gradlew test --tests "*Test" -x "*IntegrationTest" -x "*Benchmark*" --console=plain

    if [ $? -ne 0 ]; then
        print_error "快速测试失败！"
        exit 1
    fi

    print_success "快速检查完成！"
}

# 生成完整测试报告
run_full_report() {
    print_info "生成完整测试报告..."

    # 运行所有测试
    print_info "1/4 运行测试..."
    ./gradlew test --console=plain

    # 生成覆盖率报告
    print_info "2/4 生成覆盖率报告..."
    ./gradlew jacocoTestReport --console=plain

    # 生成 HTML 测试报告
    print_info "3/4 生成测试报告..."
    # JUnit 报告已自动生成

    # 运行性能基准
    print_info "4/4 运行性能基准..."
    ./gradlew test --tests "PerformanceBenchmark.generatePerformanceReport" --console=plain

    print_success "完整报告生成完成！"
    echo ""
    print_info "报告位置："
    echo "  - 测试报告: build/reports/tests/test/index.html"
    echo "  - 覆盖率:   build/reports/jacoco/test/html/index.html"
    echo ""
}

# 主逻辑
case "${1:-all}" in
    all)
        run_all_tests
        ;;
    unit)
        run_unit_tests
        ;;
    integration)
        run_integration_tests
        ;;
    benchmark)
        run_benchmark_tests
        ;;
    coverage)
        run_coverage
        ;;
    watch)
        run_watch_mode
        ;;
    quick)
        run_quick_check
        ;;
    report)
        run_full_report
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "未知选项: $1"
        echo ""
        show_help
        exit 1
        ;;
esac
