package com.fatec.cnab.servico;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fatec.cnab.model.Transacao;
import com.fatec.cnab.model.TransacaoCNAB;

@Configuration
public class BatchConfig {
	// gerenciamento da persistencia em banco de dadoss
	private PlatformTransactionManager transactionManager;
	private JobRepository jobRepository;

	// injecao via construtor
	
	public BatchConfig(PlatformTransactionManager transactionManager, JobRepository jobRepository) {
		this.transactionManager = transactionManager;
		this.jobRepository = jobRepository;
	}

	// uma tarefa pode ter varias atividades
	@Bean
	Job job(Step step, JobRepository jobRepository) {
		return new JobBuilder("job", jobRepository)
				.start(step)
				.incrementer(new RunIdIncrementer()) 
				// roda mais de uma vez
				.build();
	}
	@Bean
	Step step(ItemReader<TransacaoCNAB> reader, ItemProcessor<TransacaoCNAB, Transacao> processor, ItemWriter<Transacao> writer ) {
		//parte a ser processada 1000 itens leitura processamento escrita commmit
		return new StepBuilder("step", jobRepository)
				.<TransacaoCNAB, Transacao>chunk(1000, transactionManager)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.build();
	}
	@Bean
	FlatFileItemReader<TransacaoCNAB> reader(){
		return new FlatFileItemReaderBuilder<TransacaoCNAB>()
				.name("reader")
				.resource(new FileSystemResource("files/CNAB.txt"))
				.fixedLength() //arquivo de tamanho fixo
				.columns(
						new Range(1,1), new Range(2,9),
						new Range(10,19), new Range(20,30),
						new Range(31,42), new Range(43,48),
						new Range(49,62), new Range(63,80)
						)
				.names( //define os nomes dos campos associado ao record
					"tipo", "data","valor","cpf","cartao","hora","donoDaLoja","nomeDaLoja"	)
				.targetType(TransacaoCNAB.class)
				.build();
	}
	//Whiter pattern
	@Bean
	ItemProcessor <TransacaoCNAB, Transacao> processor(){
		return item -> {
			var transacao = new Transacao(
					null, item.tipo(),null, null, item.cpf(),
					item.cartao(),null, item.donoDaLoja().trim(),
					item.nomeDaLoja().trim())
					.withValor(
							item.valor().divide(BigDecimal.valueOf(100)))
			.withData(item.data())
			.withHora(item.hora());
			return transacao;
		};
	}
	@Bean
	JdbcBatchItemWriter<Transacao> writer (DataSource dataSource){
		return new JdbcBatchItemWriterBuilder<Transacao>()
				.dataSource(dataSource)
				.sql("""
						INSERT INTO transacao(
						tipo, data, valor, cpf, cartao,
						hora, dono_loja, nome_loja
						) VALUES(
								:tipo, :data, :valor, :cpf, :cartao,
								:hora, :donoDaLoja, :nomeDaLoja
						)
						""")
				.beanMapped()
				.build();
	}
}
