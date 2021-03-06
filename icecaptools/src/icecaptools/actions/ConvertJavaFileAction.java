package icecaptools.actions;

import icecaptools.ConverterJob;
import icecaptools.RestartableMethodObserver;
import icecaptools.compiler.ArchitectureDependentCodeDetector;
import icecaptools.compiler.EclipseCCodeFormatter;
import icecaptools.compiler.EclipseNativeMethodDetector;
import icecaptools.compiler.EclipseSourceCodeLinker;
import icecaptools.conversion.ConversionConfiguration;
import icecaptools.views.DELabelProvider;
import icecaptools.views.DependencyView;

import java.io.File;
import java.io.PrintStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

public class ConvertJavaFileAction implements IObjectActionDelegate {

    private static final String CONSOLE_NAME = "Icecap tools messages";

    private IMethod entryPoint;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    public static PrintStream getConsolePrintStream() {
        return getConsolePrintStream(CONSOLE_NAME);
    }

    public static PrintStream getConsolePrintStream(String console_name) {
        MessageConsole myConsole = null;

        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (console_name.equals(existing[i].getName())) {
                myConsole = (MessageConsole) existing[i];
                break;
            }
        }
        if (myConsole == null) {
            myConsole = new MessageConsole(console_name, null);
            conMan.addConsoles(new IConsole[] { myConsole });
        }

        IOConsoleOutputStream stream = myConsole.newOutputStream();
        PrintStream print = new PrintStream(stream);
        return print;
    }

    public static void bringConsoleToFront(String console_name, boolean clear) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (console_name.equals(existing[i].getName())) {
                conMan.showConsoleView(existing[i]);
                if (clear) {
                    if (existing[i] instanceof IOConsole) {
                        ((IOConsole) existing[i]).clearConsole();
                    }
                }
                return;
            }
        }
    }

    public static void bringConsoleToFront(boolean clear) {
        bringConsoleToFront(CONSOLE_NAME, clear);
    }

    private static PrintStream out = null;

    @Override
    public void run(IAction action) {
        if (entryPoint != null) {
            try {
                if (entryPoint.isMainMethod()) {
                    if (out == null) {
                        out = getConsolePrintStream();
                    }
                    IJavaProject javaProject = entryPoint.getJavaProject();

                    RestartableMethodObserver methodObserver = getProgressView();

                    DELabelProvider deLabelProvider = (DELabelProvider) methodObserver;

                    deLabelProvider.setLabelSource(entryPoint);

                    ConversionConfiguration config = new ConversionConfiguration();

                    try {
                        ICompilationUnit selectedJavaFile = entryPoint.getCompilationUnit();
                        String fileName = selectedJavaFile.getElementName();
                        IResource resource = selectedJavaFile.getCorrespondingResource();
                        if (resource != null) {
                            IPath fileLocation = resource.getLocation();

                            if (fileLocation != null) {
                                config.setInputSourceFileName(fileLocation.toOSString());
                            }
                        }

                        IPath outputLocation;
                        IResource projectRessource = javaProject.getCorrespondingResource();
                        config.setProjectResource(javaProject);
                        IPath projectLocation = projectRessource.getLocation().removeLastSegments(1);

                        outputLocation = javaProject.getOutputLocation();

                        StringBuffer classPath = new StringBuffer(projectLocation.toOSString() + outputLocation.toOSString());

                        String[] requiredProjects = javaProject.getRequiredProjectNames();
                        for (String project : requiredProjects) {
                            classPath.append(System.getProperty("path.separator"));
                            classPath.append(projectLocation);
                            classPath.append(File.separatorChar);
                            classPath.append(project);
                            classPath.append(File.separatorChar);
                            classPath.append("bin");
                        }

                        IClasspathEntry[] requiredLibraries = javaProject.getRawClasspath();
                        for (IClasspathEntry entry : requiredLibraries) {
                            switch (entry.getContentKind()) {
                            case IPackageFragmentRoot.K_BINARY:
                                IPath cpentry = entry.getPath();
                                String cpitem = cpentry.toOSString();
                                classPath.append(System.getProperty("path.separator"));
                                classPath.append(cpitem);
                                break;
                            case IPackageFragmentRoot.K_SOURCE:
                                entry = null;
                                break;
                            default:
                                break;
                            }
                        }

                        config.setClassPath(classPath.toString());

                        IPackageDeclaration[] packageDeclarations = selectedJavaFile.getPackageDeclarations();

                        String packageName;

                        if (packageDeclarations.length == 1) {
                            packageName = packageDeclarations[0].getElementName();
                        } else {
                            packageName = ConversionConfiguration.DEFAULT_PACKAGE_NAME;
                        }

                        config.setInputPackage(packageName);
                        config.setInputClass(fileName);
                        config.setEntryPointMethodName(entryPoint.getElementName());

                        config.setCodeFormatter(new EclipseCCodeFormatter());
                        config.setSourceCodeLinker(new EclipseSourceCodeLinker(javaProject));
                        config.setReportConversion(true);
                        config.setCodeDetector(new ArchitectureDependentCodeDetector());
                        config.setNativeMethodDetector(new EclipseNativeMethodDetector());
                        Job converterJob;

                        converterJob = new ConverterJob("Converting from " + selectedJavaFile.getElementName(), methodObserver, config, out, deLabelProvider.getCompilationRegistry());

                        converterJob.schedule();

                    } catch (JavaModelException e) {
                        out.println(e.getMessage());
                    } catch (Exception e) {
                        out.println(e.getMessage());
                    } finally {
                        out.flush();
                    }
                }
            } catch (JavaModelException e) {
                out.println(e.getMessage());
                out.flush();
            }
        }
    }

    private DependencyView getProgressView() {
        IWorkbench wb = icecaptools.Activator.getDefault().getWorkbench();
        IWorkbenchWindow activeWindow = wb.getActiveWorkbenchWindow();
        IWorkbenchPage activePage = activeWindow.getActivePage();
        IViewPart dependencyExtentViewPart = activePage.findView(DependencyView.VIEWID);
        DependencyView dependencyExtentView = null;

        if (dependencyExtentViewPart != null) {
            if (dependencyExtentViewPart instanceof DependencyView) {
                dependencyExtentView = (DependencyView) dependencyExtentViewPart;
            }
        } else {
            try {
                dependencyExtentViewPart = activePage.showView(DependencyView.VIEWID, null, org.eclipse.ui.IWorkbenchPage.VIEW_CREATE);
                if (dependencyExtentViewPart instanceof DependencyView) {
                    dependencyExtentView = (DependencyView) dependencyExtentViewPart;
                }
            } catch (PartInitException e) {
            }
        }
        if (dependencyExtentView != null) {
            activePage.bringToTop(dependencyExtentView);
        }
        return dependencyExtentView;
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection != null) {
            if (selection instanceof TreeSelection) {
                TreeSelection treeSelection = (TreeSelection) selection;
                Object firstElement = treeSelection.getFirstElement();
                if (firstElement instanceof IMethod) {
                    IMethod iMethod = (IMethod) firstElement;
                    this.entryPoint = iMethod;
                }
            }
        }
    }
}
