package management.system.sale.Repository;

import management.system.sale.Model.LogTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogTableRepository extends JpaRepository<LogTable, Integer> {

}
