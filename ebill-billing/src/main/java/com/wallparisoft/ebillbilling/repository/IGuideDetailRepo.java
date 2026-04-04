package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.GuideDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IGuideDetailRepo extends JpaRepository<GuideDetail, Long> {

  @Query("SELECT gd FROM GuideDetail gd WHERE gd.guide.idGuide = :idGuide")
  List<GuideDetail> findByGuide_IdGuide(@Param("idGuide") Long idGuide);
}
