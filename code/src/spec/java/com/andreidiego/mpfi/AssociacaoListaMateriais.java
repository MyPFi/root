package com.andreidiego.mpfi;

import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(ConcordionRunner.class)
public class AssociacaoListaMateriais {
	private List<String> sublistas = new ArrayList<String>();

	public void criarNovaSubLista(String fornecedor) {
		sublistas.add(fornecedor);
	}

	public List<String> obterSublistas() {
		return sublistas;
	}

}