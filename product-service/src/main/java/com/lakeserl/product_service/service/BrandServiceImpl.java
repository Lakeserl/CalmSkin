package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.CreateBrandRequest;
import com.lakeserl.product_service.dto.request.UpdateBrandRequest;
import com.lakeserl.product_service.dto.response.BrandDTO;
import com.lakeserl.product_service.entity.Brand;
import com.lakeserl.product_service.exception.BrandNotFoundException;
import com.lakeserl.product_service.exception.DuplicateSlugException;
import com.lakeserl.product_service.mapper.BrandMapper;
import com.lakeserl.product_service.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Override
    @Cacheable(value = "brands:all", unless = "#result == null")
    public List<BrandDTO> getAllBrands() {
        return brandRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(brandMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public BrandDTO getBrandBySlug(String slug) {
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new BrandNotFoundException("Brand not found with slug: " + slug));
                
        if (!brand.getIsActive()) {
            throw new BrandNotFoundException("Brand is not active: " + slug);
        }
        
        return brandMapper.toDto(brand);
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands:all", allEntries = true)
    public BrandDTO createBrand(CreateBrandRequest request) {
        String slug = generateSlug(request.getName());
        if (brandRepository.existsBySlug(slug)) {
            throw new DuplicateSlugException(slug);
        }

        Brand brand = brandMapper.toEntity(request);
        brand.setSlug(slug);

        return brandMapper.toDto(brandRepository.save(brand));
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands:all", allEntries = true)
    public BrandDTO updateBrand(Long id, UpdateBrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));

        brandMapper.updateEntityFromRequest(request, brand);

        if (request.getName() != null && !request.getName().equals(brand.getName())) {
            String newSlug = generateSlug(request.getName());
            if (!newSlug.equals(brand.getSlug()) && brandRepository.existsBySlug(newSlug)) {
                throw new DuplicateSlugException(newSlug);
            }
            brand.setSlug(newSlug);
        }

        return brandMapper.toDto(brandRepository.save(brand));
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands:all", allEntries = true)
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new BrandNotFoundException(id));
        // Soft delete
        brand.setIsActive(false);
        brandRepository.save(brand);
    }

    private String generateSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
