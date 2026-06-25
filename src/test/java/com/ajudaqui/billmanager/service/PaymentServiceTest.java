package com.ajudaqui.billmanager.service;

import static java.time.LocalDate.now;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import com.ajudaqui.billmanager.config.serucity.JwtUtils;
import com.ajudaqui.billmanager.controller.from.BoletoFrom;
import com.ajudaqui.billmanager.entity.*;
import com.ajudaqui.billmanager.exception.MsgException;
import com.ajudaqui.billmanager.repository.PaymentsRepository;
import com.ajudaqui.billmanager.service.vo.PayamentDto;
import com.ajudaqui.billmanager.service.vo.Sumary;
import com.ajudaqui.billmanager.utils.StatusBoleto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class PaymentServiceTest {

  @InjectMocks
  private PaymentService paymentService;
  @Mock
  private PaymentsRepository paymentsRepository;
  @Mock
  private UsersService usersService;
  @Mock
  private JwtUtils jwtUtils;

  @Mock
  private CategoryService categoryService;
  @Captor
  private ArgumentCaptor<Payment> paymentCaptor;

  @DisplayName("Deve atualizar a descrição do pagamento")
  @Test
  void shouldUpdatePaymentDescription() {
    String accessToken = "token";
    Long paymentId = 1L;
    Payment payment = new Payment();
    payment.setDescription("old");

    Users user = new Users();
    user.setCalControl(false);
    user.setAccessToken(accessToken);
    payment.setUser(user);

    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();
    BoletoFrom from = new BoletoFrom();
    from.setDescription("descrição");
    from.setValue(BigDecimal.TEN);
    from.setDueDate(LocalDate.now().plusDays(10));

    when(paymentsRepository.buscarNovasOcorrenciasDoBoletoId(paymentId)).thenReturn(List.of(payment));
    when(paymentsRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    Payment updated = paymentService.update(accessToken, paymentId, from);

    assertEquals("descrição", updated.getDescription());
  }

  @DisplayName("Deve atualizar o valor do pagamento")
  @Test
  void shouldUpdatePaymentValue() {
    String accessToken = "token";
    Long paymentId = 1L;
    Payment payment = new Payment();
    payment.setValue(BigDecimal.ONE);

    Users user = new Users();
    user.setCalControl(false);
    user.setAccessToken(accessToken);
    payment.setUser(user);

    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();
    BoletoFrom from = new BoletoFrom();
    from.setDescription("descrição");
    from.setValue(BigDecimal.TEN);
    from.setDueDate(LocalDate.now().plusDays(10));

    when(paymentsRepository.buscarNovasOcorrenciasDoBoletoId(paymentId)).thenReturn(List.of(payment));
    when(paymentsRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    Payment updated = paymentService.update(accessToken, paymentId, from);
    assertEquals(BigDecimal.TEN, updated.getValue());
  }

  @DisplayName("Deve atualizar para todas as ocorrencias")
  @Test
  void deveAtualizarParaTodasAsOcorrencias() {
    String accessToken = "token";
    Long paymentId = 1L;
    Payment payment = new Payment();
    payment.setValue(BigDecimal.ONE);

    Payment payment_2 = new Payment();
    payment.setValue(BigDecimal.ONE);

    Payment payment_3 = new Payment();
    payment.setValue(BigDecimal.ONE);

    Users user = new Users();
    user.setCalControl(false);
    user.setAccessToken(accessToken);
    payment_3.setUser(user);
    payment_2.setUser(user);
    payment.setUser(user);

    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();
    BoletoFrom from = new BoletoFrom();
    from.setDescription("descrição");
    from.setValue(BigDecimal.TEN);
    from.setDueDate(LocalDate.now().plusDays(10));

    when(paymentsRepository.buscarNovasOcorrenciasDoBoletoId(paymentId))
        .thenReturn(List.of(payment, payment_2, payment_3));
    when(paymentsRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    Payment updated = paymentService.update(accessToken, paymentId, from);
    assertEquals(BigDecimal.TEN, updated.getValue());
    verify(paymentsRepository, times(3)).save(any(Payment.class));

  }

  @DisplayName("Deve atualizar a data de vencimento do pagamento")
  @Test
  void shouldUpdatePaymentDueDate() {
    String accessToken = "token";
    Long paymentId = 1L;
    Payment payment = new Payment();
    payment.setDueDate(LocalDate.now());

    Users user = new Users();
    user.setCalControl(false);
    user.setAccessToken(accessToken);
    payment.setUser(user);

    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();
    BoletoFrom from = new BoletoFrom();
    from.setDueDate(LocalDate.now().plusDays(10));

    from.setDescription("descrição");
    from.setValue(BigDecimal.TEN);

    when(paymentsRepository.buscarNovasOcorrenciasDoBoletoId(paymentId)).thenReturn(List.of(payment));
    when(paymentsRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    Payment updated = paymentService.update(accessToken, paymentId, from);
    assertEquals(from.getDueDate(), updated.getDueDate());
  }

  @DisplayName("Deve atualizar o campo updatedAt do pagamento")
  @Test
  void shouldUpdateUpdatedAt() {
    String accessToken = "token";
    Long paymentId = 1L;
    Payment payment = new Payment();

    Users user = new Users();
    user.setCalControl(false);
    user.setAccessToken(accessToken);
    payment.setUser(user);

    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();
    BoletoFrom from = new BoletoFrom();
    from.setDescription("descrição");
    from.setValue(BigDecimal.TEN);
    from.setDueDate(LocalDate.now().plusDays(10));
    when(paymentsRepository.buscarNovasOcorrenciasDoBoletoId(paymentId)).thenReturn(List.of(payment));
    when(paymentsRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    Payment updated = paymentService.update(accessToken, paymentId, from);
    assertNotNull(updated.getUpdatedAt());
  }

  @DisplayName("Deve chamar o save no repository")
  @Test
  void shouldCallSaveOnRepository() {
    String accessToken = "token";
    Long paymentId = 1L;
    Payment payment = new Payment();
    Users user = new Users();
    user.setCalControl(false);
    user.setAccessToken(accessToken);
    payment.setUser(user);

    BoletoFrom from = new BoletoFrom();
    from.setDescription("descrição");
    from.setValue(BigDecimal.TEN);
    from.setDueDate(LocalDate.now().plusDays(10));

    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();

    when(paymentsRepository.buscarNovasOcorrenciasDoBoletoId(paymentId)).thenReturn(List.of(payment));
    when(paymentsRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    paymentService.update(accessToken, paymentId, from);
    verify(paymentsRepository).save(any(Payment.class));
  }

  @Test
  @DisplayName("Deve ter pagamento na lisat de actegoria categoria ao pagamento")
  void shouldLinkPaymentToCategory() {
    // Ambiente
    String accessToken = "";
    Long paymentId = 7L;
    Payment payment = new Payment();
    payment.setId(paymentId);

    Users users = new Users();
    users.setId(55l);
    String categoryName = "casa";
    Category category = new Category(categoryName, users);

    category.setPayments(new HashSet<Payment>());
    payment.setCategory(category);
    when(usersService.findByAccessToken(accessToken)).thenReturn(users);
    when(categoryService.findByNameOrRegister(categoryName, users)).thenReturn(category);
    when(paymentsRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    // Execução
    Payment responde = paymentService.addCategory(accessToken, paymentId, categoryName);
    // Veirifcação
    assertFalse(responde.getCategory().getPayments().isEmpty());
  }

  @Test
  @DisplayName("Deve adicionar categoria ao pagamento")
  void mustAddCategryInThePayment() {
    // Ambiente
    String accessToken = "";
    Long paymentId = 7L;
    Payment payment = new Payment();
    payment.setId(paymentId);

    Users users = new Users();
    users.setId(55l);
    String categoryName = "casa";
    Category category = new Category(categoryName, users);

    category.setPayments(new HashSet<Payment>());
    payment.setCategory(category);
    when(usersService.findByAccessToken(accessToken)).thenReturn(users);
    when(categoryService.findByNameOrRegister(categoryName, users)).thenReturn(category);
    when(paymentsRepository.findById(paymentId)).thenReturn(Optional.of(payment));
    // Execução
    Payment responde = paymentService.addCategory(accessToken, paymentId, categoryName);
    // Veirifcação
    assertNotNull(payment.getCategory());
    assertEquals(categoryName, responde.getCategory().getName());
  }

  @Test
  @DisplayName("Deve lançar uma exception se não encontrar o pagamento pelo usuáiro")
  void mustThrowExcertionIfNorForundPaymentByIdForUser() {
    // Ambiente
    Long paymentId = 7L;
    // Execução
    MsgException message = assertThrows(MsgException.class, () -> paymentService.findByIdForUsers("", paymentId));
    assertEquals(("Boleto não encontrado."), message.getMessage());
  }

  @Test
  @DisplayName("Deve buscar pagamento pelo id e user sem erro")
  void mustFindPaymentByIdAndUser() {
    // Ambiente
    Long paymentId = 7L;
    String accessToken = "";
    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();
    when(paymentsRepository.findByIdForUsers(accessToken, paymentId)).thenReturn(Optional.of(new Payment()));
    // Execução
    assertDoesNotThrow(() -> paymentService.findByIdForUsers(accessToken, paymentId));
  }

  @Test
  @DisplayName("Deve lançar uma exception se não encontrar o pagament")
  void mustThrowExcertionIfNorForundPaymentById() {
    // Ambiente
    Long paymentId = 7L;
    // Execução
    MsgException message = assertThrows(MsgException.class, () -> paymentService.findById(paymentId));
    assertEquals(("Boleto não encontrado."), message.getMessage());
  }

  @Test
  @DisplayName("Deve buscar pagamento pelo id sem erro")
  void mustFindPaymentById() {
    // Ambiente
    Long paymentId = 7L;
    when(paymentsRepository.findById(paymentId)).thenReturn(Optional.of(new Payment()));
    // Execução
    assertDoesNotThrow(() -> paymentService.findById(paymentId));
  }

  @Test
  @DisplayName("Deve trazer todos os pagamentos do usuário")
  void mustFindAllPaymentByUserId() {
    // Ambiente
    String accessToken = "";
    when(paymentsRepository.findByPaymentsForUserAccessToken(accessToken)).thenReturn(Collections.emptyList());
    // Execução
    assertDoesNotThrow(() -> paymentService.findByPayamentsForUser(accessToken));
  }

  @Test
  @DisplayName("Deve trazer os pagamentos da semana de acordo com  o status")
  void shouldBringTheWeeksPaymentsInLineWithTheStatus() {
    when(jwtUtils.getAccessTokenFromJwt(anyString())).thenCallRealMethod();

    // Execução
    List<Payment> response = paymentService.findPaymentsWeek("", now().toString(), "EM_DIAS");
    // Verificação
    assertEquals(0, response.size());
    verify(paymentsRepository, times(1))
        .findPayaments(anyString(), any(LocalDate.class), any(LocalDate.class), any(StatusBoleto.class));
    verify(paymentsRepository, times(0)).findPayaments(anyString(), any(LocalDate.class), any(LocalDate.class));
  }

  @Test
  @DisplayName("Deve trazer os pagamentos da semana sem verificar o status")
  void shouldBringUptheWeeksPaymentsWithouseCheckoingTheStatus() {
    when(jwtUtils.getAccessTokenFromJwt(anyString())).thenCallRealMethod();
    // Execução
    List<Payment> response = paymentService.findPaymentsWeek("", now().toString(), "");
    // Verificação
    assertEquals(0, response.size());
    verify(paymentsRepository, times(0))
        .findPayaments(anyString(), any(LocalDate.class), any(LocalDate.class), any(StatusBoleto.class));
    verify(paymentsRepository, times(1)).findPayaments(anyString(), any(LocalDate.class), any(LocalDate.class));
  }

  @Test
  @DisplayName("Deve retornar false se  não encontra um pagamento registrado")
  void shouldReturnsFalseIfDoesNotFindRegisteredPayment() {
    // Ambiente
    Users users = new Users();
    users.setAccessToken("");
    Payment payment = new Payment();
    payment.setUser(users);
    payment.setDueDate(LocalDate.now());
    payment.setDescription("");
    when(paymentsRepository.findPayaments(any(), any(), any())).thenReturn(Collections.emptyList());
    assertFalse(paymentService.isRegistery(payment));
  }

  @Test
  @DisplayName("Deve retornar verdadeiro se encontra um pagamento já registrado")
  void shouldReturnTrueIfPaymentHasAlreadRegistered() {
    // Ambiente
    Users users = new Users();
    users.setAccessToken("");
    Payment payment = new Payment();
    payment.setUser(users);
    payment.setDueDate(LocalDate.now());
    payment.setDescription("");
    when(paymentsRepository.findPayaments(any(), any(), any())).thenReturn(Collections.singletonList(payment));
    assertTrue(paymentService.isRegistery(payment));
  }

  @Test
  @DisplayName("Deve regsitrar uma vez com o recorrente")
  void mustRegisterOnceWithTheApplicant() {

    PayamentDto paymentDto = new PayamentDto();
    paymentDto.setDue_date(LocalDate.now());
    Long repeticao = 1L;

    String accessToken = "";
    Users user = new Users();
    when(jwtUtils.getAccessTokenFromJwt(accessToken)).thenCallRealMethod();
    when(usersService.findByAccessToken(accessToken)).thenReturn(user);

    // Execução:
    List<Payment> response = paymentService.boletosRecorrentes(paymentDto, repeticao, accessToken);

    verify(paymentsRepository, times(repeticao.intValue())).save(paymentCaptor.capture());
    List<Payment> capturedPayments = paymentCaptor.getAllValues();

    assertEquals(repeticao, capturedPayments.size());
    assertEquals(paymentDto.getDue_date(), capturedPayments.get(0).getDueDate());
    assertEquals(paymentDto.getDue_date().plusMonths(repeticao - 1),
        capturedPayments.get(capturedPayments.size() - 1).getDueDate());
    assertEquals(repeticao, response.size());
  }

  @Test
  @DisplayName("Deve regsitrar multiplos pagamentos para meses futuro")
  void shouldRegisterMultipleàymentsForFutureMonths() {

    PayamentDto paymentDto = new PayamentDto();
    paymentDto.setDescription("test boleto");
    paymentDto.setValue(new BigDecimal(23.4));
    paymentDto.setDue_date(LocalDate.now());
    Long repeticao = 7L;

    String accessToken = "";
    Users user = new Users();
    when(usersService.findByAccessToken(accessToken)).thenReturn(user);

    // Execução:
    List<Payment> response = paymentService.boletosRecorrentes(paymentDto, repeticao, accessToken);

    verify(paymentsRepository, times(repeticao.intValue())).save(paymentCaptor.capture());
    List<Payment> capturedPayments = paymentCaptor.getAllValues();

    assertEquals("test boleto", paymentCaptor.getValue().getDescription());
    assertEquals("23.40", paymentCaptor.getValue().getValue().toString());
    assertEquals(repeticao, capturedPayments.size());
    assertEquals(paymentDto.getDue_date(), capturedPayments.get(0).getDueDate());
    assertEquals(paymentDto.getDue_date().plusMonths(repeticao - 1),
        capturedPayments.get(capturedPayments.size() - 1).getDueDate());
    assertEquals(repeticao, response.size());
  }

  @Test
  @DisplayName("Deve lançar uma exception se pagamento já tiver sido registrado")
  void shouldThrowAnExceptionIfPaymentHasAlreadyRegistered() {
    PayamentDto paymentDto = new PayamentDto();
    paymentDto.setDescription("test boleto");
    paymentDto.setValue(new BigDecimal(23.4));
    paymentDto.setDue_date(now());

    String accessToken = "";
    Users user = new Users();

    when(usersService.findByAccessToken(accessToken)).thenReturn(user);
    Payment payment = paymentDto.toDatabase(user);
    payment.setUser(user);
    payment.setDueDate(now());
    when(paymentsRepository.findPayaments(any(), any(), any())).thenReturn(Arrays.asList(payment));
    // Execução:
    MsgException message = assertThrows(MsgException.class, () -> paymentService.register(paymentDto, accessToken));

    // Verificação:
    assertEquals("pagamento já cadastrado", message.getMessage());
  }

  @Test
  @DisplayName("Deve registrar um pagamento")
  void mustRegisterPayment() {
    PayamentDto paymentDto = new PayamentDto();
    paymentDto.setDescription("test boleto");
    paymentDto.setValue(new BigDecimal(23.4));
    paymentDto.setDue_date(LocalDate.now());

    String accessToken = "";
    Users user = new Users();

    when(usersService.findByAccessToken(accessToken)).thenReturn(user);
    // Execução:
    paymentService.register(paymentDto, accessToken);

    // Verificação:
    verify(paymentsRepository).save(paymentCaptor.capture());

    assertEquals("test boleto", paymentCaptor.getValue().getDescription());
    assertEquals("23.40", paymentCaptor.getValue().getValue().toString());
  }

  @Test
  @DisplayName("Deee computar o resumo")
  void shouldComputeTheSummary() {

    LocalDate start = LocalDate.now();
    LocalDate finsh = start.plusDays(30);

    String accessToken = "";
    Users user = new Users();
    List<Payment> listPayments = listPayments();
    listPayments.get(1).setStatus(StatusBoleto.VENCIDO);

    listPayments.get(2).setValue(new BigDecimal("100.00"));
    listPayments.get(2).setStatus(StatusBoleto.PAGO);

    when(usersService.findByAccessToken(accessToken)).thenReturn(user);
    when(paymentService.periodTime(accessToken, "", start, finsh, "")).thenReturn(listPayments);

    // Execução:
    Sumary response = paymentService.sumary(accessToken, start, finsh);

    // Verificação:
    // total do periodo
    assertEquals("120.20", response.getTotalDue().toString());
    // ja pago
    assertEquals("100.00", response.getAmountPaid().toString());
    // resta pagar
    assertEquals("20.20", response.getRemaining().toString());
  }

  @Test
  @DisplayName("Deve trazer o total das categorias")
  void mustComputerTheSumayByCategory() {

    LocalDate start = LocalDate.now();
    LocalDate finsh = start.plusDays(30);

    String accessToken = "";
    when(usersService.findByAccessToken(accessToken)).thenReturn(new Users());
    when(paymentService.periodTime(accessToken, "", start, finsh, "")).thenReturn(listPayments());

    // Execução:
    Map<String, BigDecimal> response = paymentService.sumaryCategory(accessToken, start, finsh);
    // Verificação
    assertEquals(2, response.size());
    assertEquals("20.20", response.get("casa").toString());
  }

  @Test
  @DisplayName("Deve trazer montante pago vazio se nenhuma conta tiver sido paga")
  void mustBringZeroAmoundPaidIfNoBillHasBeenPaid() {

    LocalDate start = LocalDate.now();
    LocalDate finsh = start.plusDays(30);

    String accessToken = "";
    Users user = new Users();
    List<Payment> listPayments = listPayments();
    listPayments.get(1).setStatus(StatusBoleto.VENCIDO);

    listPayments.get(2).setValue(new BigDecimal("100.00"));
    listPayments.get(2).setStatus(StatusBoleto.EM_DIAS);

    when(usersService.findByAccessToken(accessToken)).thenReturn(user);
    when(paymentService.periodTime(accessToken, "", start, finsh, "")).thenReturn(listPayments);

    // Execução:
    Sumary response = paymentService.sumary(accessToken, start, finsh);

    // Verificação:
    // total do periodo
    assertEquals("120.20", response.getTotalDue().toString());
    // ja pago
    assertEquals("0.00", response.getAmountPaid().toString());
    // resta pagar
    assertEquals("120.20", response.getRemaining().toString());
  }

  @Test
  @DisplayName("Deve trazer restante vazio se todas as contas tiverem sido paga")
  void mustBringAnZeroRemainingIfAllBillHaveBeenPaid() {

    LocalDate start = LocalDate.now();
    LocalDate finsh = start.plusDays(30);

    String accessToken = "";
    Users user = new Users();
    List<Payment> listPayments = listPayments();
    listPayments.get(0).setStatus(StatusBoleto.PAGO);
    listPayments.get(1).setStatus(StatusBoleto.PAGO);
    listPayments.get(2).setStatus(StatusBoleto.PAGO);

    when(usersService.findByAccessToken(accessToken)).thenReturn(user);
    when(paymentService.periodTime(accessToken, "", start, finsh, "")).thenReturn(listPayments);

    // Execução:
    Sumary response = paymentService.sumary(accessToken, start, finsh);

    // Verificação:
    // total do periodo
    assertEquals("30.30", response.getTotalDue().toString());
    // ja pago
    assertEquals("30.30", response.getAmountPaid().toString());
    // resta pagar
    assertEquals("0.00", response.getRemaining().toString());
  }

  private List<Payment> listPayments() {

    Payment payment01 = new Payment();
    payment01.setDescription("test boleto");
    payment01.setValue(new BigDecimal(10.10));
    payment01.setCategory(new Category("casa"));
    payment01.setDueDate(LocalDate.now());
    payment01.setStatus(StatusBoleto.EM_DIAS);

    Payment payment02 = new Payment();
    payment02.setDescription("test boleto");
    payment02.setCategory(new Category("casa"));
    payment02.setValue(new BigDecimal(10.10));
    payment02.setDueDate(LocalDate.now());
    payment02.setStatus(StatusBoleto.EM_DIAS);

    Payment payment03 = new Payment();
    payment03.setDescription("test boleto");
    payment03.setValue(new BigDecimal(10.10));
    payment03.setDueDate(LocalDate.now());

    payment03.setCategory(new Category("estudo"));
    payment03.setStatus(StatusBoleto.EM_DIAS);

    List<Payment> listPayments = new ArrayList<>();
    listPayments.add(payment02);
    listPayments.add(payment03);
    listPayments.add(payment01);
    return listPayments;
  }

}
