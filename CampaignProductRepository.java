package management.system.sale.Repository;

import management.system.sale.Model.CampaignProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignProductRepository extends JpaRepository<CampaignProduct, Integer> {
    List<CampaignProduct> findByCampaignId(int campaignId);
}
