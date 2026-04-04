package com.wallparisoft.authentication.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Entity
@Table(name = "menu", schema = "public")
public class Menu {

  @Id
  @SequenceGenerator(sequenceName = "menu_seq", name = "menu_seq", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "menu_seq")
  private Integer idMenu;

  // 🔁 Relación con menú padre
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id_menu")
  @JsonIgnore
  private Menu parent;

  // 🔁 Relación con submenús
  @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private List<Menu> subMenus = new ArrayList<>();

  @Column(name = "icon", length = 50)
  private String icon;

  @Column(name = "name", length = 200)
  private String name;

  @Column(name = "url", length = 50)
  private String url;

  @Column(name = "feature_code", length = 50)
  private String featureCode;

  @Column(name = "status", nullable = false)
  private Boolean status;

  @Column(name = "menu_order")
  private Integer order;
}
