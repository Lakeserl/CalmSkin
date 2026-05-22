package com.lakeserl.user_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lakeserl.user_service.exception.MaxAddressLimitException;
import com.lakeserl.user_service.exception.UserNotFoundException;
import com.lakeserl.user_service.mapper.AddressMapper;
import com.lakeserl.user_service.model.dto.request.AddressRequest;
import com.lakeserl.user_service.model.dto.response.AddressResponse;
import com.lakeserl.user_service.model.entity.User;
import com.lakeserl.user_service.model.entity.UserAddress;
import com.lakeserl.user_service.repository.UserAddressRepository;
import com.lakeserl.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Value("${app.max-addresses}")
    private int maxAddresses;

    public List<AddressResponse> getAddresses(UUID userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(UUID userId, AddressRequest request) {
        if (addressRepository.countByUserId(userId) >= maxAddresses) {
            throw new MaxAddressLimitException("Maximum " + maxAddresses + " addresses allowed");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserAddress address = addressMapper.toEntity(request);
        address.setUser(user);
        address.setCreatedAt(LocalDateTime.now());

        if (request.isDefault()) {
            clearDefaultAddress(userId);
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, AddressRequest request) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new UserNotFoundException("Address not found"));

        addressMapper.updateFromRequest(request, address);
        address.setUpdatedAt(LocalDateTime.now());

        if (request.isDefault()) {
            clearDefaultAddress(userId);
            address.setDefault(true);
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new UserNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new UserNotFoundException("Address not found"));

        clearDefaultAddress(userId);
        address.setDefault(true);
        return addressMapper.toResponse(addressRepository.save(address));
    }

    public AddressResponse getDefaultAddress(UUID userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .map(addressMapper::toResponse)
                .orElse(null);
    }

    public AddressResponse getAddressById(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .map(addressMapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("Address not found"));
    }

    private void clearDefaultAddress(UUID userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }
}
