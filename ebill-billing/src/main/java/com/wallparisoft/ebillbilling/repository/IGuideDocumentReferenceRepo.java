package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.GuideDocumentReference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IGuideDocumentReferenceRepo extends JpaRepository<GuideDocumentReference, Long> {

  @Query("SELECT gdr FROM GuideDocumentReference gdr WHERE gdr.guide.idGuide = :idGuide")
  List<GuideDocumentReference> findByGuide_IdGuide(@Param("idGuide") Long idGuide);

  List<GuideDocumentReference> findByDocumentTypeAndDocumentNumber(
      String documentType, String documentNumber);
}
