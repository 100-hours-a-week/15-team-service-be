package com.sipomeokjo.commitme.domain.company.service;

import com.sipomeokjo.commitme.api.exception.BusinessException;
import com.sipomeokjo.commitme.api.response.ErrorCode;
import com.sipomeokjo.commitme.domain.company.dto.CompanyCreateRequest;
import com.sipomeokjo.commitme.domain.company.dto.CompanyResponse;
import com.sipomeokjo.commitme.domain.company.dto.CompanyUpdateRequest;
import com.sipomeokjo.commitme.domain.company.entity.Company;
import com.sipomeokjo.commitme.domain.company.repository.CompanyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {
	
	private final CompanyRepository companyRepository;
	
	@Transactional
	public CompanyResponse create(CompanyCreateRequest request) {
		if (companyRepository.existsByName(request.name())) {
			throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
		}
		
		Company company =
				Company.create(request.name(), request.preferred(), request.idealTalent());
		
		Company savedCompany = companyRepository.save(company);
		return toResponse(savedCompany);
	}
	
	public CompanyResponse get(Long companyId) {
		Company company =
				companyRepository
						.findById(companyId)
						.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		
		return toResponse(company);
	}
	
	public List<CompanyResponse> list() {
		return companyRepository.findAll().stream().map(this::toResponse).toList();
	}
	
	@Transactional
	public CompanyResponse update(Long companyId, CompanyUpdateRequest request) {
		Company company =
				companyRepository
						.findById(companyId)
						.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		
		company.update(request.name(), request.preferred(), request.idealTalent());
		
		return toResponse(company);
	}
	
	@Transactional
	public void delete(Long companyId) {
		if (!companyRepository.existsById(companyId)) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
		}
		companyRepository.deleteById(companyId);
	}
	
	@Transactional
	public CompanyResponse verify(Long companyId, boolean verified) {
		Company company =
				companyRepository
						.findById(companyId)
						.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		
		company.verify(verified);
		return toResponse(company);
	}
	
	private CompanyResponse toResponse(Company company) {
		return new CompanyResponse(
				company.getId(),
				company.getName(),
				company.getPreferred(),
				company.getIdealTalent(),
				company.isVerified());
	}
}
