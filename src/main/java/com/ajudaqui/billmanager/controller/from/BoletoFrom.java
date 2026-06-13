package com.ajudaqui.billmanager.controller.from;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ajudaqui.billmanager.entity.Category;
import com.ajudaqui.billmanager.entity.Payment;
import com.ajudaqui.billmanager.utils.StatusBoleto;
import com.fasterxml.jackson.annotation.JsonFormat;

public class BoletoFrom {
  private String description;
  private BigDecimal value;
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate due_date;
  private String status;
  private String category;

  public Payment convert() {
    Payment payament = new Payment();
    payament.setDescription(this.description);
    payament.setValue(this.value);
    payament.setDueDate(this.due_date);
    payament.setStatus(StatusBoleto.valueOf(status));

    return payament;
  }

  public Payment toUpdate(Payment payment, Category category) {

    if (!description.isEmpty())
      payment.setDescription(description);

    if (this.value != null)
      payment.setValue(this.value);

    if (this.due_date != null)
      payment.setDueDate(this.due_date);

    if (this.category != null && !this.category.isEmpty()) {
      payment.setCategory(category);
    }
    payment.setUpdatedAt(LocalDateTime.now());
    return payment;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getValue() {
    return value;
  }

  public void setValue(BigDecimal value) {
    this.value = value;
  }

  public LocalDate getDueDate() {
    return due_date;
  }

  public void setDueDate(LocalDate dueDate) {
    this.due_date = dueDate;
  }

  public String getStatus() {
    return status;
  }

  public LocalDate getDue_date() {
    return due_date;
  }

  public void setDue_date(LocalDate due_date) {
    this.due_date = due_date;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Override
  public String toString() {
    return "BoletoFrom{description=" + description + ", category=" + category + "}";
  }

}
