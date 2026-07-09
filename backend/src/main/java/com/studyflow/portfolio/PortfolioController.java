package com.studyflow.portfolio;

import com.studyflow.common.ApiResponse;
import com.studyflow.portfolio.dto.PublicPortfolioProjectResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio/projects")
public class PortfolioController {
    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ApiResponse<List<PublicPortfolioProjectResponse>> listPublicProjects() {
        return ApiResponse.success(portfolioService.listPublicProjects());
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicPortfolioProjectResponse> getPublicProject(@PathVariable String slug) {
        return ApiResponse.success(portfolioService.getPublicProject(slug));
    }
}
