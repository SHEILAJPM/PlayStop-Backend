package com.playstop.backend.service;

import com.playstop.backend.dto.request.ProductRequest;
import com.playstop.backend.dto.response.ProductResponse;
import com.playstop.backend.entity.Branch;
import com.playstop.backend.entity.Product;
import com.playstop.backend.entity.User;
import com.playstop.backend.exception.BusinessException;
import com.playstop.backend.repository.BranchRepository;
import com.playstop.backend.repository.ProductRepository;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    // Ver mis productos (Tienda del propietario, todas sus sucursales)
    public List<ProductResponse> getMyProducts() {
        User owner = getCurrentUser();
        return productRepository.findByOwnerAndActiveTrue(owner).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse createProduct(ProductRequest request) {
        User owner = getCurrentUser();
        Branch branch = resolveBranchForWrite(owner, request.getBranchId());

        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(request.getImageUrl())
                .active(true)
                .owner(owner)
                .branch(branch)
                .build();

        return toResponse(productRepository.save(product));
    }

    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        User owner = getCurrentUser();
        Product product = getOwnedProduct(id, owner);

        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setBranch(resolveBranchForWrite(owner, request.getBranchId()));

        return toResponse(productRepository.save(product));
    }

    public void deleteProduct(UUID id) {
        User owner = getCurrentUser();
        Product product = getOwnedProduct(id, owner);

        product.setActive(false);
        productRepository.save(product);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Product getOwnedProduct(UUID id, User owner) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Producto no encontrado"));

        if (!product.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException("No tienes permiso sobre este producto");
        }
        return product;
    }

    // Resuelve y valida la sucursal opcional de un producto: si se especifica,
    // debe pertenecer al owner actual. null si no se especifica (producto sin
    // sucursal, ej. Owners fuera del plan Enterprise o sin sucursales aun).
    private Branch resolveBranchForWrite(User owner, UUID branchId) {
        if (branchId == null) return null;
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("Sucursal no encontrada"));
        if (!branch.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException("No tienes permiso sobre esta sucursal");
        }
        return branch;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .branchId(product.getBranch() != null ? product.getBranch().getId() : null)
                .branchName(product.getBranch() != null ? product.getBranch().getName() : null)
                .build();
    }
}
