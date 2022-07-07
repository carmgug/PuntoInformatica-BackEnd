package it.carmelogug.puntoinformatica.controllers.rest;


import it.carmelogug.puntoinformatica.entities.store.Product;
import it.carmelogug.puntoinformatica.entities.store.Store;
import it.carmelogug.puntoinformatica.entities.store.StoredProduct;
import it.carmelogug.puntoinformatica.services.StoreService;
import it.carmelogug.puntoinformatica.support.ResponseMessage;

import it.carmelogug.puntoinformatica.support.exceptions.Product.ProductIsBannedException;
import it.carmelogug.puntoinformatica.support.exceptions.Store.StoreAlreadyExistException;
import it.carmelogug.puntoinformatica.support.exceptions.Store.StoreIsBannedException;
import it.carmelogug.puntoinformatica.support.exceptions.Store.StoreNotExistException;
import it.carmelogug.puntoinformatica.support.exceptions.StoredProduct.StoredProductAlreadyExistException;
import it.carmelogug.puntoinformatica.support.exceptions.StoredProduct.StoredProductNotExistException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import java.util.List;


@RestController
@RequestMapping("/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @PreAuthorize("hasAuthority('puntoinformatica-admin')")
    @PostMapping
    public ResponseEntity create(@RequestBody @Valid Store store){
        try{
            Store createdStore=storeService.addStore(store);
            return new ResponseEntity(new ResponseMessage("Store added successful!",createdStore),HttpStatus.OK);
        }catch (StoreAlreadyExistException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Store already exist!",e);
        }
    }//create



    //utilizzato dal frontEnd

    @PreAuthorize("hasAuthority('puntoinformatica-admin')")
    @DeleteMapping("/{store}")
    public ResponseEntity banStore(@PathVariable(value = "store") Store store){
        try{
            Store removedStore;
            removedStore=storeService.banStore(store);
            return new ResponseEntity<>(new ResponseMessage("Store has been banned",removedStore),HttpStatus.OK);
        }catch (StoreNotExistException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Store not exist!",e);
        }
    }//delete

    @PreAuthorize("hasAuthority('puntoinformatica-admin')")
    @PutMapping("/{store}")
    public ResponseEntity unbanStore(@PathVariable(value = "store") Store store){
        try{
            Store updatedStore=storeService.unbanStore(store);
            return new ResponseEntity<>(new ResponseMessage("Store has been unbanned",updatedStore),HttpStatus.OK);
        }catch (StoreNotExistException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Store not exist!",e);
        }
    }//delete



    /*
        Utilizzata dal front end
     */
    @PreAuthorize("hasAuthority('puntoinformatica-admin')")
    @GetMapping("/search/by_varparams")
    public ResponseEntity getByCountryAndRegionAndCityAndAddress(
                @RequestParam(required = false) String country,
                @RequestParam(required = false) String region,
                @RequestParam(required = false) String city,
                @RequestParam(required = false) String province,
                @RequestParam(required = false) String address) {

        List<Store> result= storeService.showStoresByCountryAndRegionAndCityAndProvinceAndAddress(
                country,region,city,province,address);
        if(result.size()==0){
            return new ResponseEntity<>(new ResponseMessage("No result!",result),HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseMessage("Request processed!",result),HttpStatus.OK);
    }//getByCountryAndRegionAndCityAndAddress


    /*
        Permette di aggiungere un prodotto all'interno di uno store
     */

    @PreAuthorize("hasAuthority('puntoinformatica-admin')")
    @PostMapping("/{store}/{product}")
    public ResponseEntity addStoredProduct(@PathVariable(value = "store") Store store, @PathVariable(value = "product") Product product,
                                           @RequestParam @PositiveOrZero int quantity, @RequestParam @Positive double price){
        try{
            StoredProduct addedProduct=new StoredProduct();
            addedProduct.setStore(store); addedProduct.setProduct(product);
            addedProduct.setPrice(price); addedProduct.setQuantity(quantity);

            addedProduct=storeService.addStoredProduct(addedProduct);
            return new ResponseEntity<>(new ResponseMessage("Product added successful to the Store!", addedProduct),HttpStatus.OK);

        }catch (StoredProductAlreadyExistException | ProductIsBannedException | StoreIsBannedException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage(),e);
        }
    }//addStoredProduct

    @PreAuthorize("hasAuthority('puntoinformatica-admin')")
    @PutMapping("/{store}/{product}")
    public ResponseEntity updateStoredProduct(@PathVariable(value = "store") Store store, @PathVariable(value = "product") Product product,
                                           @RequestParam(required = false) @PositiveOrZero Integer quantity, @RequestParam(required = false) @Positive Double price){
        try{
            StoredProduct updatedProduct=storeService.updateStoredProduct(store,product,quantity,price);
            return new ResponseEntity<>(new ResponseMessage("StoredProduct updated successful to the Store!", updatedProduct),HttpStatus.OK);
        }catch ( StoredProductNotExistException | RuntimeException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage(),e);
        }
    }//addStoredProduct

    @PreAuthorize("hasAuthority('puntoinformatica-admin')")
    @DeleteMapping("/{store}/{product}")
    public ResponseEntity deleteStoreProduct(@PathVariable(value = "store") Store store, @PathVariable(value = "product") Product product){

        try{

            StoredProduct removedStoredProduct=storeService.removeStoredProduct(store,product);
            return new ResponseEntity<>(new ResponseMessage("Product added successful to the Store!", removedStoredProduct),HttpStatus.OK);

        }catch ( StoredProductNotExistException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage(),e);
        }
    }//deleteStoreProduct


    @PreAuthorize("hasAuthority('puntoinformatica-user')")
    @GetMapping("/storedProducts/search/getByvarParams")
    public ResponseEntity getByStoreAndProductAndPriceAndAvaible(
            @RequestBody(required = false) Store store,
            @RequestParam(required = true) int product_id,
            @RequestParam(required = false) Double price, @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable){

        List<StoredProduct> result=storeService.showStoredProductsByStoreAndProductAndPriceAndQuantity(store,product_id,price,(onlyAvailable) ? 0 :  null);
        if(result.size()==0){
            return new ResponseEntity<>(new ResponseMessage("No result!",result),HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseMessage("StoredProducts found!",result),HttpStatus.OK);
    }//getByStoreAndProductAndPriceAndAvaible



    /*
        OLD VERSION METODI DA ELIMINARE
        @PutMapping("/storedProducts/addQuantity")
    public ResponseEntity addQuantityToStoredProduct(
            @RequestBody StoredProduct storedProduct,
            @RequestParam @Positive(message = "added quantity must be positive") Integer quantity){
        StoredProduct updatedProduct;
        try {
            updatedProduct=storeService.updateQuantityStoredProduct(storedProduct,quantity);
        }catch (StoredProductNotExistException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This product doesnt exist!");
        }


        return new ResponseEntity(new ResponseMessage("Product quantity has been modifided!",updatedProduct),HttpStatus.OK);
    }

    @PutMapping("/storedProducts/modifyPrice")
    public ResponseEntity modifyPriceToStoredProduct(
            @RequestBody StoredProduct storedProduct,
            @RequestParam @Positive(message = "Price must be positive") Double price){
        StoredProduct updatedProduct;
        try {
            updatedProduct=storeService.updatePriceStoredProduct(storedProduct,price);
        }catch (StoredProductNotExistException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"This product doesnt exist!");
        }


        return new ResponseEntity(new ResponseMessage("Product price has been modifided!",updatedProduct),HttpStatus.OK);
    }

        @GetMapping("/getAll")
    public ResponseEntity getAll(){
        List<Store> result= storeService.showAllStores();
        if(result.size()==0){
            return new ResponseEntity<>(new ResponseMessage("No result!"),HttpStatus.OK);
        }
        return new ResponseEntity<>(result,HttpStatus.OK);
    }//getAll

        Handler per gestire i casi in cui è stato passato un oggetto non conforme ai vincoli esplicitati.
        Restituisce i campi della classe e il messaggio di errore associato.

        @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }





        @GetMapping("/storedProducts/search/getByvarParams")
    public ResponseEntity getByStoreAndProductAndPriceAndAvaible(
            @RequestBody Store store,
            @RequestParam(required = false) String name,@RequestParam(required = false) Product.Type type, Product.Category category,
            @RequestParam(required = false) Double price, @RequestParam(required = false, defaultValue = "false") Boolean onlyAvailable){

        name=Utilities.upperCase(name,true);
        List<StoredProduct> result=storeService.showStoredProductsByStoreAndProductAndPriceAndQuantity(
                store,
                name,type,category, //info sul prodotto
                price,(onlyAvailable) ? 0 :  null //info sullo store product
        );
        if(result.size()==0){
            return new ResponseEntity<>(new ResponseMessage("No result!"),HttpStatus.OK);
        }
        return new ResponseEntity<>(result,HttpStatus.OK);
    }
     */













}
