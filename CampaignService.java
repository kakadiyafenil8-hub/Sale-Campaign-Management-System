package management.system.sale.Service;

import management.system.sale.Model.Campaign;
import management.system.sale.Repository.CampaignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Service
public class CampaignService {
    @Autowired
    CampaignRepository campaignRepository;

    public void save_campaign(Campaign campaign) {
         campaignRepository.save(campaign);
    }

    public List<Campaign> getCampaignsByStartDate(LocalDate date) {

        return campaignRepository.findByStartDate(date);
    }
}
