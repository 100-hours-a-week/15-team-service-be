package com.sipomeokjo.commitme.domain.company.controller;

import com.sipomeokjo.commitme.domain.company.dto.CompanyCreateRequest;
import com.sipomeokjo.commitme.domain.company.dto.CompanyResponse;
import com.sipomeokjo.commitme.domain.company.dto.CompanyUpdateRequest;
import com.sipomeokjo.commitme.domain.company.service.CompanyService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
public class CompanyController {
	
	private final CompanyService companyService;
	
	@PostMapping
	public CompanyResponse create(@RequestBody @Valid CompanyCreateRequest req) {
		return companyService.create(req);
	}
	
	@GetMapping("/{companyId}")
	public CompanyResponse get(@PathVariable Long companyId) {
		return companyService.get(companyId);
	}
	
	@GetMapping
	public List<CompanyResponse> list() {
		return companyService.list();
	}
	
	@PatchMapping("/{companyId}")
	public CompanyResponse update(
			@PathVariable Long companyId, @RequestBody @Valid CompanyUpdateRequest req) {
		return companyService.update(companyId, req);
	}
	
	@DeleteMapping("/{companyId}")
	public void delete(@PathVariable Long companyId) {
		companyService.delete(companyId);
	}
	
	@PatchMapping("/{companyId}/verify")
	public CompanyResponse verify(@PathVariable Long companyId, @RequestParam boolean verified) {
		return companyService.verify(companyId, verified);
	}
}
