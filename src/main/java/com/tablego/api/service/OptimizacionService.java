package com.tablego.api.service;

import com.tablego.api.dto.OptimizacionDtos.*;
import com.tablego.api.exception.BusinessRuleException;
import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.ExpressionsBasedModel;
import org.ojalgo.optimisation.Optimisation;
import org.ojalgo.optimisation.Variable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Resuelve el modelo de Programacion Lineal Entera de reservas de Table-Go.
 *
 * Variables: x1, x2, x3 = reservas de 2, 4 y 6 personas aceptadas en el dia.
 * Maximiza Z = g1*x1 + g2*x2 + g3*x3 - CF, con gj = pj * tj * (1 - c).
 * R1: sum(pj*dj*xj) <= A*H*eta        R2: d3*x3 <= T6*H*eta
 * R3: k*sum(pj*xj) <= I               R4: xj <= Dj        R5: x3 >= q3
 * R6: xj >= 0 y enteros
 */
@Service
public class OptimizacionService {

    private static final int[] PERSONAS = {2, 4, 6};
    private static final double EPS = 1e-6;

    public OptimizacionResponse optimizar(OptimizacionRequest req) {
        GrupoRequest[] grupo = {req.grupo2(), req.grupo4(), req.grupo6()};
        int q3 = req.reservasConfirmadasDeSeis();

        if (q3 > grupo[2].demandaMaxima()) {
            throw new BusinessRuleException("Las reservas ya confirmadas de 6 personas (" + q3
                    + ") no pueden superar la demanda máxima de ese tipo (" + grupo[2].demandaMaxima() + ")");
        }

        double horas = req.horasFuncionamiento();
        double eficiencia = req.eficiencia();
        double costoFijo = req.costosFijos();

        double puestosHoraVendibles = req.aforo() * horas * eficiencia;      // A*H*eta
        double mesaHorasDeSeis = req.mesasDeSeis() * horas * eficiencia;     // T6*H*eta

        // Parametros derivados por tipo de reserva
        double[] contribucion = new double[3];        // gj
        double[] puestosHoraPorReserva = new double[3]; // pj*dj
        double[] proteinaPorReserva = new double[3];    // k*pj
        for (int j = 0; j < 3; j++) {
            double personas = PERSONAS[j];
            contribucion[j] = personas * grupo[j].ticketPorPersona() * (1 - req.costoVariable());
            puestosHoraPorReserva[j] = personas * grupo[j].duracionHoras();
            proteinaPorReserva[j] = req.consumoProteinaKg() * personas;
        }

        // ---- Modelo en ojAlgo ----
        ExpressionsBasedModel model = new ExpressionsBasedModel();

        Variable[] x = new Variable[3];
        for (int j = 0; j < 3; j++) {
            x[j] = model.addVariable("x" + PERSONAS[j])
                    .lower(0)
                    .upper(grupo[j].demandaMaxima())   // R4
                    .weight(contribucion[j]);          // funcion objetivo
            x[j].integer(true);                        // R6
        }
        x[2].lower(q3);                                // R5

        Expression r1 = model.addExpression("R1").upper(puestosHoraVendibles);
        Expression r2 = model.addExpression("R2").upper(mesaHorasDeSeis);
        Expression r3 = model.addExpression("R3").upper(req.proteinaDisponibleKg());
        for (int j = 0; j < 3; j++) {
            r1.set(x[j], puestosHoraPorReserva[j]);
            r3.set(x[j], proteinaPorReserva[j]);
        }
        r2.set(x[2], grupo[2].duracionHoras());

        Optimisation.Result result = model.maximise();
        Optimisation.State estado = result.getState();

        if (!estado.isFeasible()) {
            return new OptimizacionResponse(estado.name(), false, 0, 0, 0, 0, 0,
                    costoFijo, -costoFijo, List.of(),
                    "No existe una combinación de reservas que cumpla todas las restricciones. "
                            + "Revise los datos (reservas ya confirmadas, mesas de 6, aforo o inventario).");
        }

        // ---- Lectura de la solucion ----
        int[] n = new int[3];
        for (int j = 0; j < 3; j++) {
            n[j] = (int) Math.round(result.get(j).doubleValue());
        }

        double contribucionTotal = 0;
        int comensales = 0;
        for (int j = 0; j < 3; j++) {
            contribucionTotal += contribucion[j] * n[j];
            comensales += PERSONAS[j] * n[j];
        }
        double utilidadNeta = contribucionTotal - costoFijo;

        List<RestriccionResponse> restricciones = new ArrayList<>();
        restricciones.add(tope("R1", "Aforo y tiempo de funcionamiento", "puestos-hora",
                uso(puestosHoraPorReserva, n), puestosHoraVendibles));
        restricciones.add(tope("R2", "Mesas de 6 puestos", "mesa-horas",
                grupo[2].duracionHoras() * n[2], mesaHorasDeSeis));
        restricciones.add(tope("R3", "Inventario de proteína", "kg",
                uso(proteinaPorReserva, n), req.proteinaDisponibleKg()));
        for (int j = 0; j < 3; j++) {
            restricciones.add(tope("R4-" + PERSONAS[j],
                    "Demanda máxima de reservas de " + PERSONAS[j] + " personas", "reservas",
                    n[j], grupo[j].demandaMaxima()));
        }
        restricciones.add(minimo("R5", "Reservas de 6 personas ya confirmadas", "reservas", n[2], q3));

        List<String> limitantes = restricciones.stream()
                .filter(RestriccionResponse::saturada)
                .map(RestriccionResponse::codigo)
                .toList();

        String mensaje = "Acepte " + n[0] + " reservas de 2, " + n[1] + " de 4 y " + n[2]
                + " de 6 personas (" + comensales + " comensales). Restricciones que limitan el resultado: "
                + String.join(", ", limitantes) + ".";
        if (utilidadNeta < 0) {
            mensaje += " Atención: con estos datos la utilidad neta diaria es negativa.";
        }

        return new OptimizacionResponse(estado.name(), estado.isOptimal(), n[0], n[1], n[2], comensales,
                redondear(contribucionTotal), redondear(costoFijo), redondear(utilidadNeta),
                restricciones, mensaje);
    }

    private static double uso(double[] coeficientes, int[] n) {
        double total = 0;
        for (int j = 0; j < coeficientes.length; j++) {
            total += coeficientes[j] * n[j];
        }
        return total;
    }

    /** Restriccion del tipo "usado <= limite": saturada si ya se alcanzo el limite. */
    private static RestriccionResponse tope(String codigo, String descripcion, String unidad,
                                            double usado, double limite) {
        return new RestriccionResponse(codigo, descripcion, unidad,
                redondear(usado), redondear(limite), usado >= limite - EPS);
    }

    /** Restriccion del tipo "usado >= minimo": saturada si esta justo en el minimo. */
    private static RestriccionResponse minimo(String codigo, String descripcion, String unidad,
                                              double usado, double minimo) {
        return new RestriccionResponse(codigo, descripcion, unidad,
                redondear(usado), redondear(minimo), usado <= minimo + EPS);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}