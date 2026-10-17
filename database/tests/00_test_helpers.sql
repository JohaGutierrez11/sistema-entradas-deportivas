-- =====================================================================
-- Helpers de prueba. Se cargan al inicio de cada archivo *_tests.sql.
--
-- expect_ok(caso, sql)                 → el SQL debe ejecutarse sin error
-- expect_violation(caso, sql, regla)   → el SQL debe fallar violando
--                                         exactamente esa restricción
-- expect_true(caso, consulta)          → la consulta debe devolver TRUE
-- Los resultados se acumulan en pg_temp.test_result; report() imprime
-- el resumen y aborta con error si algún caso falló.
-- =====================================================================

CREATE TEMP TABLE test_result (case_name TEXT, passed BOOLEAN, detail TEXT);

CREATE FUNCTION pg_temp.expect_ok(p_case TEXT, p_sql TEXT) RETURNS VOID AS $$
BEGIN
    BEGIN
        EXECUTE p_sql;
        INSERT INTO pg_temp.test_result VALUES (p_case, TRUE, 'OK');
    EXCEPTION WHEN OTHERS THEN
        INSERT INTO pg_temp.test_result VALUES (p_case, FALSE, 'Error inesperado: ' || SQLERRM);
    END;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.expect_violation(p_case TEXT, p_sql TEXT, p_rule TEXT) RETURNS VOID AS $$
DECLARE
    v_constraint TEXT;
    v_column     TEXT;
    v_found      TEXT;
BEGIN
    BEGIN
        EXECUTE p_sql;
        INSERT INTO pg_temp.test_result VALUES (p_case, FALSE, 'Se aceptó, pero debía violar ' || p_rule);
    EXCEPTION WHEN integrity_constraint_violation THEN
        GET STACKED DIAGNOSTICS v_constraint = CONSTRAINT_NAME, v_column = COLUMN_NAME;
        -- NOT NULL no tiene nombre de restricción: se identifica por columna.
        v_found := COALESCE(NULLIF(v_constraint, ''), 'NOT NULL:' || v_column);
        INSERT INTO pg_temp.test_result
        VALUES (p_case, v_found = p_rule,
                CASE WHEN v_found = p_rule THEN 'Rechazado por ' || v_found
                     ELSE 'Rechazado por ' || v_found || ', se esperaba ' || p_rule END);
    END;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.expect_true(p_case TEXT, p_query TEXT) RETURNS VOID AS $$
DECLARE
    v_result BOOLEAN;
BEGIN
    EXECUTE p_query INTO v_result;
    INSERT INTO pg_temp.test_result VALUES (p_case, COALESCE(v_result, FALSE),
        CASE WHEN v_result THEN 'OK' ELSE 'La condición no se cumple' END);
EXCEPTION WHEN OTHERS THEN
    INSERT INTO pg_temp.test_result VALUES (p_case, FALSE, 'Error: ' || SQLERRM);
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION pg_temp.report() RETURNS VOID AS $$
DECLARE
    r       RECORD;
    v_total INT;
    v_fail  INT;
BEGIN
    FOR r IN SELECT * FROM pg_temp.test_result LOOP
        RAISE NOTICE '% | % | %', CASE WHEN r.passed THEN 'PASS' ELSE 'FAIL' END, r.case_name, r.detail;
    END LOOP;
    SELECT COUNT(*), COUNT(*) FILTER (WHERE NOT passed) INTO v_total, v_fail FROM pg_temp.test_result;
    RAISE NOTICE '---- % casos · % PASS · % FAIL ----', v_total, v_total - v_fail, v_fail;
    IF v_fail > 0 THEN
        RAISE EXCEPTION '% caso(s) fallaron', v_fail;
    END IF;
END;
$$ LANGUAGE plpgsql;
