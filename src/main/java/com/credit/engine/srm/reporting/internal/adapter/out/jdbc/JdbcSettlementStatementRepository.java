package com.credit.engine.srm.reporting.internal.adapter.out.jdbc;

import com.credit.engine.srm.reporting.SettlementSort;
import com.credit.engine.srm.reporting.SettlementStatement;
import com.credit.engine.srm.reporting.internal.application.SettlementSearchCriteria;
import com.credit.engine.srm.reporting.internal.application.SettlementStatementRepository;
import com.credit.engine.srm.shared.Currency;
import com.credit.engine.srm.shared.PageResult;
import com.credit.engine.srm.shared.ReceivableType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
class JdbcSettlementStatementRepository implements SettlementStatementRepository {

    private static final String SELECT = """
            select s.id as settlement_id,
                   s.batch_id,
                   s.receivable_id,
                   s.assignor_id,
                   s.assignor_document,
                   s.assignor_legal_name,
                   s.receivable_type,
                   s.due_date,
                   s.face_value,
                   s.present_value,
                   s.discount,
                   s.payment_amount,
                   s.payment_currency,
                   s.term_months,
                   s.base_rate,
                   s.spread,
                   s.exchange_base_currency,
                   s.exchange_quote_currency,
                   s.exchange_rate,
                   s.exchange_effective_at,
                   s.exchange_captured_at,
                   s.pricing_date,
                   s.calculated_at,
                   s.settled_at
              from settlements s
            """;

    private final JdbcClient jdbc;

    JdbcSettlementStatementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult<SettlementStatement> search(SettlementSearchCriteria criteria) {
        QueryParts query = queryParts(criteria);
        long total = jdbc.sql("select count(*) from settlements s" + query.whereClause())
                .params(query.parameters())
                .query(Long.class)
                .single();

        Map<String, Object> pageParameters = new LinkedHashMap<>(query.parameters());
        pageParameters.put("limit", criteria.size());
        pageParameters.put("offset", Math.multiplyExact((long) criteria.page(), criteria.size()));
        String sql = SELECT + query.whereClause()
                + " order by " + orderBy(criteria.sort())
                + " limit :limit offset :offset";
        List<SettlementStatement> content = jdbc.sql(sql)
                .params(pageParameters)
                .query(JdbcSettlementStatementRepository::map)
                .list();
        int totalPages = total == 0
                ? 0
                : Math.toIntExact(((total - 1) / criteria.size()) + 1);
        return new PageResult<>(content, criteria.page(), criteria.size(), total, totalPages);
    }

    private static QueryParts queryParts(SettlementSearchCriteria criteria) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (criteria.fromInclusive() != null) {
            where.append(" and s.settled_at >= :fromInclusive");
            parameters.put("fromInclusive", utc(criteria.fromInclusive()));
        }
        if (criteria.toExclusive() != null) {
            where.append(" and s.settled_at < :toExclusive");
            parameters.put("toExclusive", utc(criteria.toExclusive()));
        }
        if (criteria.assignorId() != null) {
            where.append(" and s.assignor_id = :assignorId");
            parameters.put("assignorId", criteria.assignorId());
        }
        if (criteria.paymentCurrency() != null) {
            where.append(" and s.payment_currency = :paymentCurrency");
            parameters.put("paymentCurrency", criteria.paymentCurrency().name());
        }
        return new QueryParts(where.toString(), Map.copyOf(parameters));
    }

    private static String orderBy(SettlementSort sort) {
        String column = switch (sort.field()) {
            case SETTLED_AT -> "s.settled_at";
            case ASSIGNOR_LEGAL_NAME -> "s.assignor_legal_name";
            case PAYMENT_AMOUNT -> "s.payment_amount";
        };
        String direction = switch (sort.direction()) {
            case ASC -> "asc";
            case DESC -> "desc";
        };
        return column + " " + direction + ", s.id " + direction;
    }

    private static SettlementStatement map(ResultSet resultSet, int rowNumber) throws SQLException {
        Currency paymentCurrency = Currency.valueOf(resultSet.getString("payment_currency"));
        return new SettlementStatement(
                resultSet.getObject("settlement_id", java.util.UUID.class),
                resultSet.getObject("batch_id", java.util.UUID.class),
                resultSet.getObject("receivable_id", java.util.UUID.class),
                resultSet.getObject("assignor_id", java.util.UUID.class),
                resultSet.getString("assignor_document"),
                resultSet.getString("assignor_legal_name"),
                ReceivableType.valueOf(resultSet.getString("receivable_type")),
                resultSet.getObject("due_date", java.time.LocalDate.class),
                money(resultSet, "face_value", Currency.BRL),
                money(resultSet, "present_value", Currency.BRL),
                money(resultSet, "discount", Currency.BRL),
                money(resultSet, "payment_amount", paymentCurrency),
                resultSet.getInt("term_months"),
                resultSet.getBigDecimal("base_rate"),
                resultSet.getBigDecimal("spread"),
                exchangeRate(resultSet),
                resultSet.getObject("pricing_date", java.time.LocalDate.class),
                instant(resultSet, "calculated_at"),
                instant(resultSet, "settled_at"));
    }

    private static SettlementStatement.MoneySnapshot money(
            ResultSet resultSet,
            String column,
            Currency currency) throws SQLException {
        return new SettlementStatement.MoneySnapshot(resultSet.getBigDecimal(column), currency);
    }

    private static SettlementStatement.ExchangeRateSnapshot exchangeRate(ResultSet resultSet)
            throws SQLException {
        String base = resultSet.getString("exchange_base_currency");
        if (base == null) {
            return null;
        }
        return new SettlementStatement.ExchangeRateSnapshot(
                Currency.valueOf(base),
                Currency.valueOf(resultSet.getString("exchange_quote_currency")),
                resultSet.getBigDecimal("exchange_rate"),
                instant(resultSet, "exchange_effective_at"),
                instant(resultSet, "exchange_captured_at"));
    }

    private static java.time.Instant instant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record QueryParts(String whereClause, Map<String, Object> parameters) {
    }
}
