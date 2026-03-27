package com.moneyflow.domain.budget;

import java.time.LocalDate;
import java.util.UUID;

public record ExpenseCreatedEvent(UUID accountBookId, LocalDate expenseDate) {}
