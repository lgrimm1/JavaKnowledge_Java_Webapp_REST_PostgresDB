package lgrimm1.JavaKnowledge.Common;

import lgrimm1.JavaKnowledge.Html.*;
import lgrimm1.JavaKnowledge.Process.*;
import lgrimm1.JavaKnowledge.Title.*;
import lgrimm1.JavaKnowledge.Txt.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.lang.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.servlet.*;

import java.io.*;
import java.util.*;

@Service
public class CommonService {

	private final TitleRepository titleRepository;
	private final TxtRepository txtRepository;
	private final HtmlRepository htmlRepository;
	private final Formulas formulas;
	private final ProcessRecords processRecords;
	private final FileOperations fileOperations;
	private final Extractors extractors;
	private final ProcessPage processPage;
	private final HtmlGenerators htmlGenerators;

	@Autowired
	public CommonService(TitleRepository titleRepository,
						 TxtRepository txtRepository,
						 HtmlRepository htmlRepository,
						 Formulas formulas,
						 ProcessRecords processRecords,
						 FileOperations fileOperations,
						 Extractors extractors,
						 ProcessPage processPage,
						 HtmlGenerators htmlGenerators) {
		this.titleRepository = titleRepository;
		this.txtRepository = txtRepository;
		this.htmlRepository = htmlRepository;
		this.formulas = formulas;
		this.processRecords = processRecords;
		this.fileOperations = fileOperations;
		this.extractors = extractors;
		this.processPage = processPage;
		this.htmlGenerators = htmlGenerators;
	}

	public ModelAndView getRoot(String initialView, ModelAndView modelAndView) {
//		ModelAndView modelAndView = new ModelAndView(initialView);
		modelAndView.setViewName(initialView);
		modelAndView.addObject("search_text", "");
		return modelAndView;
	}

	/**
	 * Finds titles which contain any word of the given text, furthermore
	 * titles of which their TXT content contains the whole given text.
	 * The search trims the given text and ignores case.
	 * In case there is no search text given, returns all titles.
	 */
	public ModelAndView searchPages(String searchText, @NonNull ModelAndView modelAndView) {
		if (searchText == null || searchText.isBlank()) {
			modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
		}
		else {
			Set<String> titles = processRecords.searchBySearchText(searchText, titleRepository, txtRepository);
			modelAndView.addObject(
					"titles",
					titles.stream()
							.sorted()
							.toList()
			);
		}
		return modelAndView;
	}

	public ModelAndView getPage(List<String> titles, @NonNull ModelAndView modelAndView) {
		//TODO open one or more static pages
		return modelAndView;
	}

	public ModelAndView managePages(@NonNull ModelAndView modelAndView) {
		modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
		modelAndView.addObject("files", new ArrayList<File>());
		modelAndView.addObject("confirm", false);
		modelAndView.addObject("message", "");
		return modelAndView;
	}

	public ModelAndView createSourcePage(@NonNull ModelAndView modelAndView) {
		modelAndView.addObject("title", "");
		modelAndView.addObject("file_name", "");
		modelAndView.addObject("content", new ArrayList<String>());
		modelAndView.addObject("edit_existing_page", false);
		modelAndView.addObject("message", "");
		return modelAndView;
	}

	public ModelAndView editSourcePage(List<String> titles, @NonNull ModelAndView modelAndView) {
		if (titles == null || titles.size() != 1 || titles.get(0) == null || titles.get(0).isBlank()) {
			modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
			modelAndView.addObject("files", new ArrayList<File>());
			modelAndView.addObject("confirm", false);
			modelAndView.addObject("message", "Please select exactly one title for editing.");
			modelAndView.setViewName("management");
		}
		else {
			Optional<TitleEntity> optionalTitleEntity = titleRepository.findByTitle(titles.get(0));
			if (optionalTitleEntity.isEmpty()) {
				modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
				modelAndView.addObject("files", new ArrayList<File>());
				modelAndView.addObject("confirm", false);
				modelAndView.addObject("message",
						"Please select exactly one title for editing.");
				modelAndView.setViewName("management");
			}
			else {
				long txtId = optionalTitleEntity.get().getTxtId();
				Optional<TxtEntity> optionalTxtEntity = txtRepository.findById(txtId);
				if (optionalTxtEntity.isEmpty()) {
					modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
					modelAndView.addObject("files", new ArrayList<File>());
					modelAndView.addObject("confirm", false);
					modelAndView.addObject("message",
							"Please select exactly one title for editing.");
					modelAndView.setViewName("management");
				}
				else {
					modelAndView.addObject("title", optionalTitleEntity.get().getTitle());
					modelAndView.addObject("file_name", optionalTitleEntity.get().getFilename());
					modelAndView.addObject("content", optionalTxtEntity.get().getContent());
					modelAndView.addObject("edit_existing_page", true);
					modelAndView.addObject("message", "");
				}
			}
		}
		return modelAndView;
	}

	public ModelAndView deletePages(List<String> titles, Boolean confirm, ModelAndView modelAndView) {
		if (titles == null || titles.size() == 0 || !confirm) {
			modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
			modelAndView.addObject("files", new ArrayList<File>());
			modelAndView.addObject("confirm", false);
			if (!confirm) {
				modelAndView.addObject("message", "Please confirm deletion.");
			}
			else {
				modelAndView.addObject("message", "Please select titles you wish to delete.");
			}
		}
		else {
			titles = titles.stream()
					.filter(title -> title != null && !title.isBlank())
					.toList();
			if (titles.isEmpty()) {
				modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
				modelAndView.addObject("files", new ArrayList<File>());
				modelAndView.addObject("confirm", false);
				modelAndView.addObject("message",
						"Please select existing titles you wish to delete.");
			}
			else {
				long numberOfGivenTitles = titles.size();
				modelAndView.addObject(
						"message",
						processRecords.deleteByTitles(
								titles,
								titleRepository,
								txtRepository,
								htmlRepository) +
								" of " + numberOfGivenTitles + " titles were deleted.");
				modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
				modelAndView.addObject("files", new ArrayList<File>());
				modelAndView.addObject("confirm", false);
			}
		}
		return modelAndView;
	}

	public ModelAndView publishPages(ModelAndView modelAndView) {
		modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
		modelAndView.addObject("files", new ArrayList<File>());
		modelAndView.addObject("confirm", false);
		long[] publishResults = processRecords.publishHtml(titleRepository, htmlRepository, fileOperations);
		modelAndView.addObject("message",
				publishResults[0] + " HTML files were pre-deleted, " + publishResults[1] + " were published.");
		return modelAndView;
	}

	public ModelAndView addFormula(String formulaName,
								   String title,
								   String fileName,
								   List<String> content,
								   Boolean editExistingPage,
								   ModelAndView modelAndView) {
		if (title == null || title.isBlank()) {
			title = "";
		}
		if (fileName == null || fileName.isBlank()) {
			fileName = "";
		}
		if (content == null) {
			content = new ArrayList<>();
		}
		if (editExistingPage == null) {
			editExistingPage = false;
		}
		String formula = formulas.getFormula(formulaName);
		if (formula.isEmpty()) {
			modelAndView.addObject("message", "Wrong formula name was asked.");
		}
		else {
			content.addAll(List.of(formula.split("\n")));
			modelAndView.addObject("message", "Formula was appended.");
		}
		modelAndView.addObject("title", title);
		modelAndView.addObject("file_name", fileName);
		modelAndView.addObject("content", content);
		modelAndView.addObject("edit_existing_page", editExistingPage);
		return modelAndView;
	}

	public ModelAndView savePage(String title,
								 String fileName,
								 List<String> content,
								 Boolean editExistingPage,
								 ModelAndView modelAndView) {
		if (title == null || title.isBlank()) {
			title = "";
			modelAndView.addObject("message", "Please define a title.");
		}
		else if (fileName == null || fileName.isBlank()) {
			fileName = "";
			modelAndView.addObject("message", "Please define a file name.");
		}
		else {
			if (content == null) {
				content = new ArrayList<>();
			}
			if (editExistingPage == null) {
				editExistingPage = false;
			}
			Optional<TitleEntity> optionalTitleEntity = titleRepository.findByTitle(title);
			if (editExistingPage) {
				if (optionalTitleEntity.isEmpty()) {
					modelAndView.addObject("message", "There is no existing page with this title.");
				}
				else {
					txtRepository.deleteById(optionalTitleEntity.get().getTxtId());
					htmlRepository.deleteById(optionalTitleEntity.get().getHtmlId());
					titleRepository.deleteById(optionalTitleEntity.get().getId());
					HtmlEntity htmlEntity = htmlRepository.save(new HtmlEntity(new ArrayList<>()));
					TxtEntity txtEntity = txtRepository.save(new TxtEntity(content));
					titleRepository.save(new TitleEntity(title, fileName, txtEntity.getId(), htmlEntity.getId()));
					modelAndView.addObject("message", "Source page has been overwritten.");
				}
			}
			else { //editExistingPage == false
				if (optionalTitleEntity.isPresent()) {
					modelAndView.addObject("message", "There is an existing page with this title.");
				}
				else {
					HtmlEntity htmlEntity = htmlRepository.save(new HtmlEntity(new ArrayList<>()));
					TxtEntity txtEntity = txtRepository.save(new TxtEntity(content));
					titleRepository.save(new TitleEntity(title, fileName, txtEntity.getId(), htmlEntity.getId()));
					editExistingPage = true;
					modelAndView.addObject("message", "Source page has been saved.");
				}
			}
		}
		modelAndView.addObject("title", title);
		modelAndView.addObject("file_name", fileName);
		modelAndView.addObject("content", content);
		modelAndView.addObject("edit_existing_page", editExistingPage);
		return modelAndView;
	}

	public ModelAndView importTxt(List<File> files, Boolean confirm, ModelAndView modelAndView) {
		if (files == null || files.isEmpty() || confirm == null || !confirm) {
			modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
			modelAndView.addObject("files", new ArrayList<File>());
			modelAndView.addObject("confirm", false);
			modelAndView.addObject("message",
					"Please upload minimum one file and confirm source overwriting.");
		}
		else {
			List<File> notImportedFiles = processRecords.importTxtFiles(
					files,
					titleRepository,
					txtRepository,
					htmlRepository,
					fileOperations,
					formulas,
					extractors);
			List<String> titles = processRecords.getAllTitles(titleRepository);
			modelAndView.addObject("titles", titles);
			modelAndView.addObject("files", new ArrayList<File>());
			modelAndView.addObject("confirm", false);
			modelAndView.addObject("message",
					notImportedFiles.size() + " of " + files.size() + " files were not imported.");
		}
		return modelAndView;
	}

	public ModelAndView generateHtml(Boolean confirm, ModelAndView modelAndView) {
		if (confirm == null || !confirm) {
			modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
			modelAndView.addObject("files", new ArrayList<File>());
			modelAndView.addObject("confirm", false);
			modelAndView.addObject("message", "Please confirm generating pages.");
		}
		else {
			long[] messageData = processRecords.generate(
					titleRepository,
					txtRepository,
					htmlRepository,
					formulas,
					processPage,
					extractors,
					htmlGenerators);
			modelAndView.addObject("titles", processRecords.getAllTitles(titleRepository));
			modelAndView.addObject("files", new ArrayList<File>());
			modelAndView.addObject("confirm", false);
			modelAndView.addObject("message",
					messageData[0] + " pages in " + messageData[1] + " seconds has been processed.");
		}
		return modelAndView;
	}
}

//TODO check all Stream operations for being parallel and its necessity.
//TODO exportTxt()
