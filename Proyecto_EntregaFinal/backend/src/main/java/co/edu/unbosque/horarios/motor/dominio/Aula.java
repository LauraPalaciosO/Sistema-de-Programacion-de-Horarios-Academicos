package co.edu.unbosque.horarios.motor.dominio;

import java.util.Set;

public record Aula(int id, String codigo, int capacidad, Set<String> caracteristicas) {

    public boolean cumple(Set<String> requisitos) {
        return caracteristicas.containsAll(requisitos);
    }
}
