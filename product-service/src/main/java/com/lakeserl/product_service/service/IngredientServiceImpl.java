package com.lakeserl.product_service.service;

import com.lakeserl.product_service.dto.request.CheckSafeRequest;
import com.lakeserl.product_service.dto.request.CreateIngredientRequest;
import com.lakeserl.product_service.dto.request.UpdateIngredientRequest;
import com.lakeserl.product_service.dto.response.IngredientConflictDTO;
import com.lakeserl.product_service.dto.response.IngredientDTO;
import com.lakeserl.product_service.dto.response.IngredientSafetyDTO;
import com.lakeserl.product_service.entity.Ingredient;
import com.lakeserl.product_service.entity.IngredientConflict;
import com.lakeserl.product_service.entity.ProductIngredient;
import com.lakeserl.product_service.enums.IngredientSafetyLevel;
import com.lakeserl.product_service.exception.IngredientNotFoundException;
import com.lakeserl.product_service.mapper.IngredientMapper;
import com.lakeserl.product_service.repository.IngredientConflictRepository;
import com.lakeserl.product_service.repository.IngredientRepository;
import com.lakeserl.product_service.repository.ProductIngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientConflictRepository conflictRepository;
    private final IngredientMapper ingredientMapper;
    private final ProductIngredientRepository productIngredientRepository;

    @Override
    public List<IngredientDTO> getAllIngredients() {
        return ingredientRepository.findAll().stream()
                .map(ingredientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public IngredientDTO getIngredientById(Long id) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        return ingredientMapper.toDto(ingredient);
    }

    @Override
    public List<IngredientDTO> getIngredientsByProductId(Long productId) {
        List<ProductIngredient> links = productIngredientRepository.findByProductIdOrderByDisplayOrderAsc(productId);
        return links.stream()
                .map(ProductIngredient::getIngredient)
                .map(ingredientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<IngredientSafetyDTO> checkIngredientsSafety(CheckSafeRequest request) {
        List<Ingredient> ingredients = ingredientRepository.findByNameIn(request.getIngredientNames());
        List<IngredientSafetyDTO> safetyList = new ArrayList<>();

        for (Ingredient ingredient : ingredients) {
            IngredientSafetyLevel status = ingredient.getSafetyLevel();
            String reason = "Safe to use";

            if (request.getSkinType() != null && ingredient.getSuitableSkinTypes() != null) {
                if (!ingredient.getSuitableSkinTypes().contains(request.getSkinType().name()) && !ingredient.getSuitableSkinTypes().contains("ALL")) {
                    status = IngredientSafetyLevel.CAUTION;
                    reason = "May not be suitable for " + request.getSkinType().name() + " skin";
                }
            }
            
            if (Boolean.TRUE.equals(ingredient.getIsCommonAllergen())) {
                status = IngredientSafetyLevel.CAUTION;
                reason = "Common allergen. Patch test recommended.";
            }

            safetyList.add(IngredientSafetyDTO.builder()
                    .ingredientName(ingredient.getName())
                    .status(status)
                    .reason(reason)
                    .build());
        }

        return safetyList;
    }

    @Override
    public List<IngredientConflictDTO> checkConflicts(List<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.size() < 2) {
            return new ArrayList<>();
        }
        
        List<IngredientConflict> conflicts = conflictRepository.findConflictsBetween(ingredientIds);
        
        return conflicts.stream()
                .map(c -> IngredientConflictDTO.builder()
                        .ingredientA(c.getIngredientA().getName())
                        .ingredientB(c.getIngredientB().getName())
                        .severity(c.getSeverity())
                        .reason(c.getReason())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public IngredientDTO createIngredient(CreateIngredientRequest request) {
        Ingredient ingredient = ingredientMapper.toEntity(request);
        return ingredientMapper.toDto(ingredientRepository.save(ingredient));
    }

    @Override
    @Transactional
    public IngredientDTO updateIngredient(Long id, UpdateIngredientRequest request) {
        Ingredient ingredient = ingredientRepository.findById(id)
                .orElseThrow(() -> new IngredientNotFoundException(id));
        
        ingredientMapper.updateEntityFromRequest(request, ingredient);
        return ingredientMapper.toDto(ingredientRepository.save(ingredient));
    }

    @Override
    @Transactional
    public void deleteIngredient(Long id) {
        if (!ingredientRepository.existsById(id)) {
            throw new IngredientNotFoundException(id);
        }
        ingredientRepository.deleteById(id);
    }
}
