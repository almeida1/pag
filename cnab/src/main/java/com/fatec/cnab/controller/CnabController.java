package com.fatec.cnab.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fatec.cnab.servico.CnabService;

@RestController
@RequestMapping("/api/v1/cnab")
public class CnabController {
	CnabService service;
	public CnabController(CnabService service) {
		this.service = service;
	}
	
	@PostMapping("/upload")
	public String upload(@RequestParam("file") MultipartFile file) throws Exception{
		service.uploadCnabFile(file);
		return "Processamento iniciado";
	}

}
