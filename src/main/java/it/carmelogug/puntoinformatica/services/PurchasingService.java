package it.carmelogug.puntoinformatica.services;

import it.carmelogug.puntoinformatica.entities.purchasing.Cart;
import it.carmelogug.puntoinformatica.entities.purchasing.StoredProductInCart;
import it.carmelogug.puntoinformatica.entities.purchasing.StoredProductInPurchase;
import it.carmelogug.puntoinformatica.entities.purchasing.Purchase;
import it.carmelogug.puntoinformatica.entities.store.StoredProduct;
import it.carmelogug.puntoinformatica.entities.User;
import it.carmelogug.puntoinformatica.repositories.CartRepository;
import it.carmelogug.puntoinformatica.repositories.StoredProductInCartRepository;
import it.carmelogug.puntoinformatica.repositories.StoredProductInPurchaseRepository;
import it.carmelogug.puntoinformatica.repositories.PurchaseRepository;
import it.carmelogug.puntoinformatica.support.exceptions.Purchasing.*;
import it.carmelogug.puntoinformatica.support.exceptions.StoredProduct.StoredProductNotExistException;
import it.carmelogug.puntoinformatica.support.exceptions.User.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import java.util.Date;

import java.util.List;

@Service
public class PurchasingService {



    @Autowired
    private PurchaseRepository purchaseRepository;
    @Autowired
    private StoredProductInPurchaseRepository storedProductInPurchaseRepository;
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private StoredProductInCartRepository storedProductInCartRepository;

    @PersistenceContext
    private EntityManager entityManager;



/*  Old
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRES_NEW, rollbackFor = QuantityProductUnvailableException.class)
    public Purchase addPurchase(Purchase purchase) throws QuantityProductUnvailableException, StoredProductNotExistException {
        Purchase res=purchaseRepository.save(purchase);
        double totalPrice=0;
        for(StoredProductInPurchase pip: purchase.getProductsInPurchase()){
            StoredProduct storedProduct=pip.getStoredProduct();

            storedProduct=entityManager.find(StoredProduct.class,storedProduct.getId());
            if(storedProduct==null) throw new StoredProductNotExistException();

            entityManager.lock(storedProduct, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            int newQuantity=storedProduct.getQuantity()-pip.getQuantity();
            if ( newQuantity<0){
                throw new QuantityProductUnvailableException(
                        "Quantity Product Unvailable!"+","+
                        "Product: " + storedProduct.getProduct().toString()+","+
                        "Available: " + storedProduct.getQuantity()
                );
            }
            storedProduct.setQuantity(newQuantity);

            pip.setPrice(pip.getQuantity()* storedProduct.getPrice());
            pip.setStoredProduct(storedProduct);
            pip.setPurchase(res);
            storedProductInPurchaseRepository.save(pip);//insert record in database

            totalPrice+= pip.getPrice();
        }
        entityManager.refresh(res);
        res.setPrice(totalPrice);
        res=entityManager.merge(res);//update record in database
        return res;
    }

 */
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRES_NEW, rollbackFor = QuantityProductUnvailableException.class)
    public Purchase addPurchase(Cart cart) throws QuantityProductUnvailableException, CartIsEmptyException {
        entityManager.refresh(cart);
        entityManager.lock(cart,LockModeType.PESSIMISTIC_WRITE);
        if(cart.getStoredProductsInCart().size()==0) throw new CartIsEmptyException();

        Purchase result=purchaseRepository.save(new Purchase());
        double totalPrice=0;
        for(StoredProductInCart currp: cart.getStoredProductsInCart()){
            StoredProduct storedProduct=currp.getStoredProduct();

            entityManager.lock(storedProduct, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

            int newQuantity=storedProduct.getQuantity()-currp.getQuantity();

            if ( newQuantity<0){
                throw new QuantityProductUnvailableException(
                        "Quantity Product Unvailable!"+","+
                                "Product: " + storedProduct.getProduct().toString()+","+
                                "Available: " + storedProduct.getQuantity()
                );
            }
            storedProduct.setQuantity(newQuantity);
            StoredProductInPurchase pip=new StoredProductInPurchase();
            pip.setQuantity(currp.getQuantity());
            pip.setPrice(currp.getQuantity()*storedProduct.getPrice());
            pip.setStoredProduct(storedProduct);
            pip.setPurchase(result);
            storedProductInPurchaseRepository.save(pip);//insert record in database
            storedProductInCartRepository.delete(currp); //element has been sold
            totalPrice+= pip.getPrice();
        }
        entityManager.refresh(result);
        result.setBuyer(cart.getBuyer());
        result.setPrice(totalPrice);
        result=entityManager.merge(result);//update record in database
        return result;
    }

    @Transactional(readOnly = true)
    public List<Purchase> getPurchasesByUserInPeriod(User user, Date startDate, Date endDate) throws UserNotFoundException, DateWrongRangeException {

        if(startDate!=null && endDate!=null && startDate.after(endDate)) throw new DateWrongRangeException();

        if(startDate==null) startDate=new Date(0); //1970-01-01
        if(endDate==null) endDate=new Date(System.currentTimeMillis());

        User currUser=entityManager.find(User.class,user.getId());
        if(currUser==null) throw new UserNotFoundException();
        List<Purchase> result= purchaseRepository.getPurchasesByBuyerAndPurchaseTimeBetweenStartDateAndEndDate(user,startDate,endDate);

        return result;
    }


    /*
        Gestione Carrello
     */

    @Transactional(readOnly = false)
    public Cart addCart(User user) throws UserNotFoundException, CartAlreadyExistException {
        User currUser=entityManager.find(User.class,user.getId());
        if(currUser==null) throw new UserNotFoundException();
        if(cartRepository.existsByBuyer(currUser)) throw new CartAlreadyExistException();

        Cart cart=new Cart();
        cart.setBuyer(currUser);
        cart=cartRepository.save(cart);
        return cart;
    }
    @Transactional(readOnly = true)
    public Cart getCart(User user) throws CartNotExistException {
        Cart currCart=cartRepository.findCartByBuyer(user);
        if(currCart==null) throw new CartNotExistException();
        return currCart;

    }


    @Transactional(readOnly = false)
    public Cart addStoredProductToCart(User user,StoredProduct storedProduct,int quantity) throws UserNotFoundException, StoredProductNotExistException, CartNotExistException {
        //Check--> Probabilmente i controlli sulla ricerca dell'utente non dovrebbero essere fatti!!
        User currUser=entityManager.find(User.class,user.getId());
        if(currUser==null) throw new UserNotFoundException();

        storedProduct=entityManager.find(StoredProduct.class,storedProduct.getId());
        if(storedProduct==null) throw new StoredProductNotExistException();

        Cart currCart=cartRepository.findCartByBuyer(currUser);
        if(currCart==null) throw new CartNotExistException();


        StoredProductInCart addedElement=storedProductInCartRepository.findStoredProductInCartByCartAndStoredProduct(currCart,storedProduct);
        if(addedElement!=null) {
            int newQuantity= addedElement.getQuantity()+quantity;
            addedElement.setQuantity(newQuantity);
        }
        else {
            addedElement=new StoredProductInCart();
            addedElement.setCart(currCart);
            addedElement.setStoredProduct(storedProduct);
            addedElement.setQuantity(quantity);
        }
        storedProductInCartRepository.save(addedElement);
        entityManager.refresh(currCart);
        return currCart;
    }

    @Transactional(readOnly = false)
    public Cart removeStoredProductFromCart(User user,StoredProduct storedProduct) throws CartNotExistException, StoredProductNotInCart {
        Cart currCart=cartRepository.findCartByBuyer(user);
        if(currCart==null) throw new CartNotExistException();
        StoredProductInCart storedProductInCart=storedProductInCartRepository.findStoredProductInCartByCartAndStoredProduct(currCart,storedProduct);
        if(storedProductInCart==null) throw new StoredProductNotInCart();
        storedProductInCartRepository.delete(storedProductInCart);
        entityManager.refresh(currCart);
        return currCart;
    }



}