package kz.iitu.springlab.aspect;

import kz.iitu.springlab.service.CatalogService;
import kz.iitu.springlab.web.CatalogController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class CatalogAspectsIntegrationTest {
    @Autowired
    private CatalogService catalogService;

    @Autowired
    private CatalogController controller;

    private static List<String> adviceLines(CapturedOutput output) {
        return output.getAll().lines()
                .filter(line -> line.contains("[AUDIT]") || line.contains("[LOG]")
                        || line.contains("[TIME]") || line.contains("[TRACE]"))
                .toList();
    }

    @Test
    void slowAuditedCallHasFiveRecordsInDeclaredAspectOrder(CapturedOutput output) {
        assertThat(catalogService.findAll(3)).containsExactly("Item no. 1", "Item no. 2", "Item no. 3");

        List<String> lines = adviceLines(output);
        assertThat(lines).hasSize(5);
        assertThat(lines.get(0)).contains("[AUDIT] start CATALOG_LIST", "timestamp=", "args=[3]");
        assertThat(lines.get(1)).contains("[LOG] -> CatalogService.findAll(..)", "args=[3]");
        assertThat(lines.get(2)).contains("WARN", "[TIME] SLOW: CatalogService.findAll(..)");
        assertThat(lines.get(3)).contains("[LOG] <- CatalogService.findAll(..)",
                "returned [Item no. 1, Item no. 2, Item no. 3]");
        assertThat(lines.get(4)).contains("[AUDIT] CATALOG_LIST success", "timestamp=");
    }

    @Test
    void unannotatedLookupIsLoggedAndTimedButNotAudited(CapturedOutput output) {
        assertThat(catalogService.findById(5)).isEqualTo("Item no. 5");

        List<String> lines = adviceLines(output);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).contains("[LOG] -> CatalogService.findById(..)", "args=[5]");
        assertThat(lines.get(1)).contains("[TIME]", "CatalogService.findById(..)");
        assertThat(lines.get(2)).contains("[LOG] <- CatalogService.findById(..)", "returned Item no. 5");
        assertThat(lines).noneMatch(line -> line.contains("[AUDIT]"));
    }

    @Test
    void invalidRemovalIsTimedAuditedAndRethrownWithoutAfterReturning(CapturedOutput output) {
        assertThatThrownBy(() -> catalogService.remove(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid identifier: 0");

        List<String> lines = adviceLines(output);
        assertThat(lines).hasSize(5);
        assertThat(lines.get(0)).contains("[AUDIT] start CATALOG_REMOVE", "timestamp=").doesNotContain("args=");
        assertThat(lines.get(1)).contains("[LOG] -> CatalogService.remove(..)", "args=[0]");
        assertThat(lines.get(2)).contains("[TIME]", "CatalogService.remove(..)");
        assertThat(lines.get(3)).contains("[LOG] !! CatalogService.remove(..)",
                "IllegalArgumentException: Invalid identifier: 0");
        assertThat(lines.get(4)).contains("[AUDIT] CATALOG_REMOVE failure", "timestamp=",
                "IllegalArgumentException: Invalid identifier: 0");
        assertThat(lines).noneMatch(line -> line.contains("[LOG] <-"));
    }

    @Test
    void selfInvocationSkipsBothInternalAuditsWhileLoggingOuterCall(CapturedOutput output) {
        assertThat(catalogService.removeTwice(5))
                .isEqualTo("Removed item no. 5; Removed item no. 6");

        List<String> lines = adviceLines(output);
        assertThat(lines).hasSize(4);
        assertThat(lines).noneMatch(line -> line.contains("[AUDIT]"));
        assertThat(lines.stream().filter(line -> line.contains("[LOG]")).toList()).hasSize(2);
        assertThat(lines).anyMatch(line -> line.contains("[TRACE] @After finally CatalogService.removeTwice(..)"));
        assertThat(lines).noneMatch(line -> line.contains("CatalogService.remove(..)"));
    }

    @Test
    void separateServiceBeanFixInterceptsEachRemovalWithAllMatchingAspects(CapturedOutput output) {
        assertThat(catalogService.removeTwiceFixed(5))
                .isEqualTo("Removed item no. 5; Removed item no. 6");

        List<String> lines = adviceLines(output);
        assertThat(lines).hasSize(14);
        assertThat(lines.stream().filter(line -> line.contains("[AUDIT] start CATALOG_REMOVE")).toList()).hasSize(2);
        assertThat(lines.stream().filter(line -> line.contains("[AUDIT] CATALOG_REMOVE success")).toList()).hasSize(2);
        assertThat(lines.stream().filter(line -> line.contains("[LOG]")).toList()).hasSize(6);
        assertThat(lines.stream().filter(line -> line.contains("[TIME]")).toList()).hasSize(3);
        assertThat(lines.stream().filter(line -> line.contains("[LOG] -> CatalogRemovalService.remove(..)")).toList()).hasSize(2);
        assertThat(lines).anyMatch(line -> line.contains("[TRACE] @After finally CatalogService.removeTwiceFixed(..)"));
    }

    @Test
    void afterAdviceRunsEvenWhenTheSelfInvocationDemoFails(CapturedOutput output) {
        assertThatThrownBy(() -> catalogService.removeTwice(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid identifier: 0");

        List<String> lines = adviceLines(output);
        assertThat(lines).hasSize(4);
        assertThat(lines).anyMatch(line -> line.contains("[TRACE] @After finally CatalogService.removeTwice(..)"));
        assertThat(lines).anyMatch(line -> line.contains("[LOG] !! CatalogService.removeTwice(..)"));
        assertThat(lines).noneMatch(line -> line.contains("[AUDIT]") || line.contains("[LOG] <-"));
    }

    @Test
    void controllerReturnsBadRequestAfterTheExceptionHasPassedThroughAdvice(CapturedOutput output) throws Exception {
        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockMvc.perform(delete("/api/lab4/item/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid identifier: 0"));

        assertThat(adviceLines(output)).anyMatch(line -> line.contains("[AUDIT] CATALOG_REMOVE failure"));
        assertThat(adviceLines(output)).anyMatch(line -> line.contains("[LOG] !! CatalogService.remove(..)"));
    }

    @Test
    void proxyEndpointReportsTheActualSpringCglibProxy() throws Exception {
        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockMvc.perform(get("/api/lab4/proxy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superClass").value("CatalogService"))
                .andExpect(jsonPath("$.isAopProxy").value(true))
                .andExpect(jsonPath("$.isCglib").value(true));
        assertThat(controller.proxyInfo().get("className").toString()).contains("CatalogService$$SpringCGLIB$$");
    }
}
