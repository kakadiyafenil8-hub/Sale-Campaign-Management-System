package management.system.sale.Scheduler;

import management.system.sale.Model.Campaign;
import management.system.sale.Model.CampaignProduct;
import management.system.sale.Model.LogTable;
import management.system.sale.Model.Product;
import management.system.sale.Repository.CampaignProductRepository;
import management.system.sale.Repository.CampaignRepository;
import management.system.sale.Repository.LogTableRepository;
import management.system.sale.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class extra {

    @Autowired
    CampaignRepository campaignRepository;

    @Autowired
    CampaignProductRepository campaignProductRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    LogTableRepository logTableRepository;


    @Scheduled(cron = "0 32 10 * * *")
    public void checkCampaign() {

        LocalDate today = LocalDate.now();

        List<Campaign> campaigns = campaignRepository.findAll();


        // =========================================================
        // START CAMPAIGN
        // =========================================================

        List<Thread> startThreads = new ArrayList<>();

        for (Campaign campaign : campaigns) {

            if (campaign.getStartDate().equals(today)) {

                List<CampaignProduct> campaignProducts =
                        campaignProductRepository
                                .findByCampaignId(campaign.getId());

                for (CampaignProduct cp : campaignProducts) {

                    Thread thread = new Thread(() -> {

                        int maxRetries = 3;

                        for (int attempt = 1;
                             attempt <= maxRetries;
                             attempt++) {

                            try {

                                // =========================================
                                // PREVENT DUPLICATE CAMPAIGN
                                // =========================================

                                if ("ACTIVE".equals(cp.getStatus())
                                        || "ENDED".equals(cp.getStatus())) {

                                    return;
                                }


                                // =========================================
                                // FIND PRODUCT
                                // =========================================

                                Product product =
                                        productRepository
                                                .findById(cp.getProductId())
                                                .orElse(null);

                                if (product == null) {

                                    System.out.println(
                                            "Product not found: "
                                                    + cp.getProductId()
                                    );

                                    return;
                                }


                                // =========================================
                                // PRICE CALCULATION
                                // =========================================

                                /*
                                 *
                                 * Example:
                                 *
                                 * First campaign:
                                 *
                                 * 906
                                 * ↓
                                 * 906 - 9.33%
                                 * ↓
                                 * 821.47
                                 *
                                 *
                                 * Second campaign:
                                 *
                                 * 821.47
                                 * ↓
                                 * 821.47 - 10%
                                 * ↓
                                 * 739.32
                                 *
                                 */

                                double oldPrice =
                                        product.getCurrentPrice();

                                double campaignPrice =
                                        oldPrice
                                                - (oldPrice
                                                * cp.getDiscount()
                                                / 100);


                                // =========================================
                                // UPDATE CAMPAIGN PRODUCT
                                // =========================================

                                cp.setOldPrice(oldPrice);

                                cp.setCampaignPrice(campaignPrice);

                                cp.setStatus("ACTIVE");

                                campaignProductRepository.save(cp);


                                // =========================================
                                // UPDATE PRODUCT
                                // =========================================

                                product.setCurrentPrice(
                                        campaignPrice
                                );

                                product.setDiscount(
                                        cp.getDiscount()
                                );

                                productRepository.save(product);


                                // =========================================
                                // SAVE LOG
                                // =========================================

                                LogTable logTable =
                                        new LogTable();

                                logTable.setProduct_id(
                                        product.getId()
                                );

                                logTable.setCampaign_id(
                                        campaign.getId()
                                );

                                logTable.setOldPrice(
                                        oldPrice
                                );

                                logTable.setNewPrice(
                                        campaignPrice
                                );

                                logTable.setChangeType(
                                        "CAMPAIGN_STARTED"
                                );

                                logTableRepository.save(
                                        logTable
                                );


                                // =========================================
                                // SUCCESS
                                // =========================================

                                System.out.println(
                                        "START SUCCESS | Product: "
                                                + product.getId()
                                                + " | Attempt: "
                                                + attempt
                                );

                                // Successfully completed
                                break;


                            } catch (Exception e) {

                                System.out.println(
                                        "START ERROR | Product: "
                                                + cp.getProductId()
                                                + " | Attempt: "
                                                + attempt
                                );

                                System.out.println(
                                        "Reason: "
                                                + e.getMessage()
                                );


                                // =========================================
                                // RETRY
                                // =========================================

                                if (attempt < maxRetries) {

                                    System.out.println(
                                            "Retrying START | Product: "
                                                    + cp.getProductId()
                                                    + " | Next attempt: "
                                                    + (attempt + 1)
                                    );

                                    try {

                                        Thread.sleep(1000);

                                    } catch (InterruptedException ex) {

                                        Thread.currentThread()
                                                .interrupt();

                                        return;
                                    }

                                } else {

                                    // =====================================
                                    // ALL 3 ATTEMPTS FAILED
                                    // =====================================

                                    System.out.println(
                                            "START FAILED after 3 attempts"
                                                    + " | Product: "
                                                    + cp.getProductId()
                                                    + " | SKIPPED"
                                    );
                                }
                            }
                        }

                    });


                    startThreads.add(thread);

                    thread.start();
                }
            }
        }


        // =========================================================
        // WAIT FOR ALL START THREADS
        // =========================================================

        for (Thread thread : startThreads) {

            try {

                thread.join();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.out.println(
                        "START thread interrupted: "
                                + e.getMessage()
                );
            }
        }


        // =========================================================
        // END CAMPAIGN
        // =========================================================

        List<Thread> endThreads = new ArrayList<>();

        for (Campaign campaign : campaigns) {

            if (campaign.getEndDate().equals(today)) {

                List<CampaignProduct> campaignProducts =
                        campaignProductRepository
                                .findByCampaignId(campaign.getId());

                for (CampaignProduct cp : campaignProducts) {

                    Thread thread = new Thread(() -> {

                        int maxRetries = 3;

                        for (int attempt = 1;
                             attempt <= maxRetries;
                             attempt++) {

                            try {

                                // =========================================
                                // ONLY ACTIVE CAMPAIGN CAN END
                                // =========================================

                                if (!"ACTIVE".equals(cp.getStatus())) {

                                    return;
                                }


                                // =========================================
                                // FIND PRODUCT
                                // =========================================

                                Product product =
                                        productRepository
                                                .findById(cp.getProductId())
                                                .orElse(null);

                                if (product == null) {

                                    System.out.println(
                                            "Product not found: "
                                                    + cp.getProductId()
                                    );

                                    return;
                                }


                                double priceBeforeEnding =
                                        product.getCurrentPrice();


                                // =========================================
                                // MARK CAMPAIGN AS ENDED
                                // =========================================

                                cp.setStatus("ENDED");

                                campaignProductRepository.save(cp);


                                // =========================================
                                // FIND ALL CAMPAIGNS
                                // =========================================

                                List<CampaignProduct>
                                        allProductCampaigns =
                                        campaignProductRepository
                                                .findAll();


                                // =========================================
                                // FIND ORIGINAL PRICE
                                // =========================================

                                double originalPrice =
                                        cp.getOldPrice();

                                CampaignProduct
                                        firstCampaignProduct = null;

                                LocalDate firstStartDate = null;


                                for (CampaignProduct productCp :
                                        allProductCampaigns) {

                                    if (productCp.getProductId()
                                            != product.getId()) {

                                        continue;
                                    }


                                    Campaign productCampaign =
                                            campaignRepository
                                                    .findById(
                                                            productCp
                                                                    .getCampaignId()
                                                    )
                                                    .orElse(null);

                                    if (productCampaign == null) {
                                        continue;
                                    }


                                    if (firstStartDate == null
                                            || productCampaign
                                            .getStartDate()
                                            .isBefore(firstStartDate)) {

                                        firstStartDate =
                                                productCampaign
                                                        .getStartDate();

                                        firstCampaignProduct =
                                                productCp;
                                    }
                                }


                                if (firstCampaignProduct != null) {

                                    originalPrice =
                                            firstCampaignProduct
                                                    .getOldPrice();
                                }


                                // =========================================
                                // CALCULATE ACTIVE CAMPAIGNS
                                // =========================================

                                double newPrice =
                                        originalPrice;

                                int activeCampaignCount = 0;

                                double totalDiscount = 0;


                                for (CampaignProduct productCp :
                                        allProductCampaigns) {

                                    if (productCp.getProductId()
                                            != product.getId()) {

                                        continue;
                                    }


                                    if (!"ACTIVE".equals(
                                            productCp.getStatus())) {

                                        continue;
                                    }


                                    /*
                                     * Apply remaining campaign
                                     * discount sequentially.
                                     */

                                    newPrice =
                                            newPrice
                                                    - (newPrice
                                                    * productCp
                                                    .getDiscount()
                                                    / 100);

                                    activeCampaignCount++;
                                }


                                // =========================================
                                // UPDATE PRODUCT PRICE
                                // =========================================

                                product.setCurrentPrice(
                                        newPrice
                                );


                                if (activeCampaignCount == 0) {

                                    // =====================================
                                    // NO ACTIVE CAMPAIGN
                                    // =====================================

                                    product.setCurrentPrice(
                                            originalPrice
                                    );

                                    product.setDiscount(0);


                                } else {

                                    // =====================================
                                    // ACTIVE CAMPAIGNS REMAIN
                                    // =====================================

                                    totalDiscount =
                                            ((originalPrice - newPrice)
                                                    / originalPrice)
                                                    * 100;

                                    product.setDiscount(
                                            totalDiscount
                                    );
                                }


                                productRepository.save(product);


                                // =========================================
                                // SAVE END LOG
                                // =========================================

                                LogTable logTable =
                                        new LogTable();

                                logTable.setProduct_id(
                                        product.getId()
                                );

                                logTable.setCampaign_id(
                                        campaign.getId()
                                );

                                logTable.setOldPrice(
                                        priceBeforeEnding
                                );

                                logTable.setNewPrice(
                                        newPrice
                                );

                                logTable.setChangeType(
                                        "CAMPAIGN_ENDED"
                                );

                                logTableRepository.save(
                                        logTable
                                );


                                // =========================================
                                // SUCCESS
                                // =========================================

                                System.out.println(
                                        "END SUCCESS | Product: "
                                                + product.getId()
                                                + " | Attempt: "
                                                + attempt
                                );

                                break;


                            } catch (Exception e) {

                                System.out.println(
                                        "END ERROR | Product: "
                                                + cp.getProductId()
                                                + " | Attempt: "
                                                + attempt
                                );

                                System.out.println(
                                        "Reason: "
                                                + e.getMessage()
                                );


                                // =========================================
                                // RETRY
                                // =========================================

                                if (attempt < maxRetries) {

                                    System.out.println(
                                            "Retrying END | Product: "
                                                    + cp.getProductId()
                                                    + " | Next attempt: "
                                                    + (attempt + 1)
                                    );

                                    try {

                                        Thread.sleep(1000);

                                    } catch (InterruptedException ex) {

                                        Thread.currentThread()
                                                .interrupt();

                                        return;
                                    }

                                } else {

                                    // =====================================
                                    // ALL 3 ATTEMPTS FAILED
                                    // =====================================

                                    System.out.println(
                                            "END FAILED after 3 attempts"
                                                    + " | Product: "
                                                    + cp.getProductId()
                                                    + " | SKIPPED"
                                    );
                                }
                            }
                        }

                    });


                    endThreads.add(thread);

                    thread.start();
                }
            }
        }


        // =========================================================
        // WAIT FOR ALL END THREADS
        // =========================================================

        for (Thread thread : endThreads) {

            try {

                thread.join();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.out.println(
                        "END thread interrupted: "
                                + e.getMessage()
                );
            }
        }


        // =========================================================
        // SAVE CAMPAIGNS
        // =========================================================

        for (Campaign campaign : campaigns) {

            campaignRepository.save(campaign);
        }


        System.out.println(
                "Campaign scheduler completed."
        );
    }
}