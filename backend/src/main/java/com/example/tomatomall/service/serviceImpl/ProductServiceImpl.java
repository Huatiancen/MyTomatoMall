package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.*;
import com.example.tomatomall.repository.*;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.util.UserContext;
import com.example.tomatomall.vo.ProductVO;
import com.example.tomatomall.vo.SpecificationVO;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SpecificationRepository specificationRepository;

    @Autowired
    private StockpileRepository StockpileRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserContext userContext;

    @Override
    public List<ProductVO> getAllProductsWithSpecifications() {
        List<Product> products = productRepository.findAll();
        return transferProductVOS(products);
    }

    @Override
    public ProductVO getSpecificProductWithSpecifications(String id) {
        Product product = productRepository.findById(Integer.parseInt(id)).get();
        ProductVO productVO = product.toVO();

        List<Specification> specifications = specificationRepository.findByProduct(product);
        List<SpecificationVO> specVOs = new ArrayList<>();

        for (Specification spec : specifications) {
            SpecificationVO specVO = new SpecificationVO();
            specVO.setId(spec.getId());
            specVO.setItem(spec.getItem());
            specVO.setValue(spec.getValue());
            specVO.setProductId(product.getId());
            specVOs.add(specVO);
        }

        productVO.setSpecifications(specVOs);

        return productVO;
    }

    @Override
    public String updateProduct(ProductVO productVO) {
        Product product = productRepository.findById(productVO.getId()).get();
        if (product.getId() == null) {
            throw TomatoMallException.productNotExist();
        }
        product.setTitle(productVO.getTitle());
        product.setPrice(productVO.getPrice());
        product.setRate(productVO.getRate());
        product.setDescription(productVO.getDescription());
        product.setCover(productVO.getCover());
        product.setDetail(productVO.getDetail());
        productRepository.save(product);
        if (productVO.getSpecifications() == null) {
            return "更新成功";
        }
        List<SpecificationVO> specVOs = productVO.getSpecifications();
        for (SpecificationVO specVO : specVOs) {
            Specification spec = specificationRepository.findById(specVO.getId()).get();
            spec.setItem(specVO.getItem());
            spec.setValue(specVO.getValue());
            specificationRepository.save(spec);
        }
        return "更新成功";
    }

    @Override
    public ProductVO createProduct(ProductVO productVO) {
        if (productRepository.findByTitle(productVO.getTitle()) != null) {
            throw TomatoMallException.productAlreadyExist();
        }
        Product product = productVO.toPO();
        product = productRepository.save(product);
        List<SpecificationVO> specVOs = productVO.getSpecifications();
        if (specVOs == null) {
            return product.toVO();
        }
        for (SpecificationVO specVO : specVOs) {
            Specification spec = new Specification();
            spec.setItem(specVO.getItem());
            spec.setValue(specVO.getValue());
            spec.setProduct(product);
            specificationRepository.save(spec);
        }
        return product.toVO();
    }

    @Override
    public String deleteProduct(String id) {
        if (!productRepository.findById(Integer.parseInt(id)).isPresent()) {
            throw TomatoMallException.productNotExist();
        }
        Product product = productRepository.findById(Integer.parseInt(id)).get();
        List<Specification> specifications = specificationRepository.findByProduct(product);
        specificationRepository.deleteAll(specifications);
        // 可能会有库存信息关联，这里采用先删库存再删商品的方式，也可以在PO中设置级联删除
        Stockpile stockpile = StockpileRepository.findByProduct(product);
        if (stockpile != null) {
            StockpileRepository.delete(stockpile);
        }
        productRepository.delete(product);
        return "删除成功";
    }

    @Override
    public String updateStockpile(String productId, Stockpile stockpile) {
        Product product = productRepository.findById(Integer.parseInt(productId)).get();
        if (product.getId() == null) {
            throw TomatoMallException.productNotExist();
        }
        Stockpile stockpile1 = StockpileRepository.findByProduct(product);
        if (stockpile1 == null) { // 如果库存不存在，则创建新的库存记录
            stockpile1 = new Stockpile();
            stockpile1.setProduct(product);
            stockpile1.setFrozen(0);
            stockpile1.setAmount(stockpile.getAmount());
            StockpileRepository.save(stockpile1);
            return "调整库存成功";
        }
        stockpile1.setAmount(stockpile.getAmount());
        stockpile1.setId(stockpile1.getId());
        stockpile1.setProduct(product);
        stockpile1.setFrozen(stockpile1.getFrozen());
        StockpileRepository.save(stockpile1);
        return "调整库存成功";
    }

    @Override
    public Stockpile getStockpile(String productId) {
        if (!productRepository.findById(Integer.parseInt(productId)).isPresent()) {
            throw TomatoMallException.productNotExist();
        }
        Product product = productRepository.findById(Integer.parseInt(productId)).get();
        return StockpileRepository.findByProduct(product);
    }

    @Override
    public List<String> getProductTags(String productId) {
        if (!productRepository.findById(Integer.parseInt(productId)).isPresent()) {
            throw TomatoMallException.productNotExist();
        }
        Product product = productRepository.findById(Integer.parseInt(productId)).get();
        return product.getTags();
    }

    @Override
    public String updateProductTags(String productId, List<String> tags) {
        if (!productRepository.findById(Integer.parseInt(productId)).isPresent()) {
            throw TomatoMallException.productNotExist();
        }
        Product product = productRepository.findById(Integer.parseInt(productId)).get();
        product.setTags(tags);
        productRepository.save(product);
        return "标签更新成功";
    }

    @Override
    public List<ProductVO> filterProducts(List<String> tags) {
        List<Product> products;
        if (tags.size() == 1) {
            products = productRepository.findByAnyTagIn(tags);
        } else if (tags.size() == 2) {
            products = productRepository.findByAllTagsIn(tags, 2L);
        } else {
            throw TomatoMallException.tagsTooLong();
        }
        return transferProductVOS(products);
    }

    @Override
    public List<ProductVO> getLatestProducts(Integer limit) {
        List<Product> products = productRepository.findTopNByCreateTimeDesc(limit);
        return transferProductVOS(products);
    }

    @Override
    public List<ProductVO> getTopRatedProducts(Integer limit) {
        List<Product> products = productRepository.findTopNBySalesVolumeDesc(limit);
        return transferProductVOS(products);
    }

    @Override
    public List<ProductVO> getRecommendedProducts(Integer limit) {
        // 获取当前用户
        Account currentUser = userContext.getCurrentUser();
        if (currentUser == null) {
            return getTopRatedProducts(limit);
        }

        // 使用顺序表存储推荐商品
        List<Product> recommendedProducts = new ArrayList<>();
        Set<Integer> addedProductIds = new HashSet<>();

        // 1. 获取用户最近订单中的商品和购物车中的商品
        List<Product> recentProducts = getRecentOrderProducts(currentUser.getId());
        // 2. 获取用户购物车中的商品
        List<Product> cartProducts = getCartProducts(currentUser);

        // 获取购物车商品ID集合，用于过滤
        Set<Integer> cartProductIds = cartProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        // 获取所有可用商品
        List<Product> allActiveProducts = productRepository.findByStatus("ACTIVE");

        // 2. 优先匹配最近订单中商品的相似商品
        for (Product recentProduct : recentProducts) {
            List<Product> similarProducts = findSimilarProductsByName(recentProduct, allActiveProducts, cartProductIds,
                    addedProductIds);
            for (Product similar : similarProducts) {
                if (recommendedProducts.size() >= limit)
                    break;
                recommendedProducts.add(similar);
                addedProductIds.add(similar.getId());
            }
            if (recommendedProducts.size() >= limit)
                break;
        }

        // 3. 再匹配购物车中商品的相似商品
        if (recommendedProducts.size() < limit) {
            for (Product cartProduct : cartProducts) {
                List<Product> similarProducts = findSimilarProductsByName(cartProduct, allActiveProducts,
                        cartProductIds, addedProductIds);
                for (Product similar : similarProducts) {
                    if (recommendedProducts.size() >= limit)
                        break;
                    recommendedProducts.add(similar);
                    addedProductIds.add(similar.getId());
                }
                if (recommendedProducts.size() >= limit)
                    break;
            }
        }

        // 4. 如果仍然不足，使用标签匹配
        if (recommendedProducts.size() < limit) {
            Set<String> userTags = new HashSet<>();
            recentProducts.forEach(p -> {
                if (p.getTags() != null)
                    userTags.addAll(p.getTags());
            });
            cartProducts.forEach(p -> {
                if (p.getTags() != null)
                    userTags.addAll(p.getTags());
            });

            if (!userTags.isEmpty()) {
                List<Product> tagBasedProducts = productRepository.findByAnyTagIn(new ArrayList<>(userTags))
                        .stream()
                        .filter(p -> "ACTIVE".equals(p.getStatus()))
                        .filter(p -> !cartProductIds.contains(p.getId()))
                        .filter(p -> !addedProductIds.contains(p.getId()))
                        .collect(Collectors.toList());

                for (Product tagProduct : tagBasedProducts) {
                    if (recommendedProducts.size() >= limit)
                        break;
                    recommendedProducts.add(tagProduct);
                    addedProductIds.add(tagProduct.getId());
                }
            }
        }

        // 5. 如果还是不足，补充热门商品
        if (recommendedProducts.size() < limit) {
            int needed = limit - recommendedProducts.size();
            List<Product> topProducts = productRepository.findTopNBySalesVolumeDesc(needed * 2)
                    .stream()
                    .filter(p -> "ACTIVE".equals(p.getStatus()))
                    .filter(p -> !cartProductIds.contains(p.getId()))
                    .filter(p -> !addedProductIds.contains(p.getId()))
                    .limit(needed)
                    .collect(Collectors.toList());

            recommendedProducts.addAll(topProducts);
        }

        // 限制返回数量
        if (recommendedProducts.size() > limit) {
            recommendedProducts = recommendedProducts.subList(0, limit);
        }

        return transferProductVOS(recommendedProducts);
    }

    // 获取用户最近订单中的商品
    private List<Product> getRecentOrderProducts(Integer userId) {
        List<Order> recentOrders = orderRepository.findTop5ByUserIdOrderByCreateTimeDesc(userId);
        Set<Product> products = new HashSet<>();

        for (Order order : recentOrders) {
            for (Cart item : order.getCartItems()) {
                products.add(item.getProduct());
            }
        }

        return new ArrayList<>(products);
    }

    // 获取用户购物车中的商品
    private List<Product> getCartProducts(Account user) {
        List<Cart> cartItems = cartRepository.findByUser(user);
        return cartItems.stream()
                .map(Cart::getProduct)
                .collect(Collectors.toList());
    }

    private List<ProductVO> transferProductVOS(List<Product> products) {
        List<ProductVO> productVOs = new ArrayList<>();

        for (Product product : products) {
            ProductVO productVO = product.toVO();
            List<Specification> specifications = specificationRepository.findByProduct(product);
            List<SpecificationVO> specVOs = new ArrayList<>();

            for (Specification spec : specifications) {
                SpecificationVO specVO = new SpecificationVO();
                specVO.setId(spec.getId());
                specVO.setItem(spec.getItem());
                specVO.setValue(spec.getValue());
                specVO.setProductId(product.getId());
                specVOs.add(specVO);
            }

            productVO.setSpecifications(specVOs);
            productVOs.add(productVO);
        }

        return productVOs;
    }

    // 基于商品名称查找相似商品
    private List<Product> findSimilarProductsByName(Product targetProduct, List<Product> allProducts,
            Set<Integer> cartProductIds, Set<Integer> addedProductIds) {
        String targetName = targetProduct.getTitle().toLowerCase();
        List<Product> similarProducts = new ArrayList<>();

        for (Product product : allProducts) {
            // 跳过购物车中的商品和已添加的商品
            if (cartProductIds.contains(product.getId()) || addedProductIds.contains(product.getId())) {
                continue;
            }

            String productName = product.getTitle().toLowerCase();
            double similarity = calculateNameSimilarity(targetName, productName);

            // 相似度阈值设为0.25
            if (similarity >= 0.25) {
                similarProducts.add(product);
            }
        }

        // 按相似度排序（降序）
        similarProducts.sort((p1, p2) -> {
            double sim1 = calculateNameSimilarity(targetName, p1.getTitle().toLowerCase());
            double sim2 = calculateNameSimilarity(targetName, p2.getTitle().toLowerCase());
            return Double.compare(sim2, sim1);
        });

        return similarProducts;
    }

    // 计算字符串相似度（使用简单的Jaccard相似度）
    private double calculateNameSimilarity(String name1, String name2) {
        if (name1.equals(name2)) {
            return 1.0;
        }

        // 去除标点符号，保留字母数字和中文
        String cleanName1 = name1.replaceAll("[^\\w\\u4e00-\\u9fa5]", "").toLowerCase();
        String cleanName2 = name2.replaceAll("[^\\w\\u4e00-\\u9fa5]", "").toLowerCase();

        // 处理边界情况
        if (cleanName1.isEmpty() || cleanName2.isEmpty()) {
            return 0.0;
        }

        if (cleanName1.length() < 2 || cleanName2.length() < 2) {
            // 对于长度小于2的字符串，直接比较是否相等
            return cleanName1.equals(cleanName2) ? 1.0 : 0.0;
        }

        // 分词处理（简单按空格和常见分隔符分割）
        Set<String> words1 = new HashSet<>();
        Set<String> words2 = new HashSet<>();

        // 将字符串按字符分割，也可以考虑按词分割
        for (int i = 0; i < cleanName1.length() - 2; i++) {
            words1.add(cleanName1.substring(i, i + 2));
        }
        for (int i = 0; i < cleanName2.length() - 2; i++) {
            words2.add(cleanName2.substring(i, i + 2));
        }

        // 计算Jaccard相似度
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    @Override
    public String publishProduct(String id) {
        if (!(productRepository.findById(Integer.parseInt(id)).isPresent())) {
            throw TomatoMallException.productNotExist();
        }
        Product product = productRepository.findById(Integer.parseInt(id)).get();
        if (product.getStatus().equals("ACTIVE")) {
            product.setStatus("INACTIVE");
        } else if (product.getStatus().equals("INACTIVE")) {
            product.setStatus("ACTIVE");
        }
        productRepository.save(product);
        return "修改上下架状态成功";
    }
}