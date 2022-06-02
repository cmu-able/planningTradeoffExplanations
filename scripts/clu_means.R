clu_means<-function(x, id, disp=TRUE, center=TRUE, scale=TRUE, cluster_names = NULL){
  clu = NULL
  x = data.frame(scale(as.matrix(x), center = center, scale = scale), stringsAsFactors = TRUE)
  
  p=ncol(x)
  gm=apply(x,2,mean)
  
  id=factor(id)
  csize=as.vector(table(id)/sum(table(id)))
  
  x$clu=id
  clum=(x %>% group_by(clu) %>% summarise_all(mean))
  
  am=rbind(clum[,-1],gm)
  bm=data.frame(t(am),stringsAsFactors = TRUE)
  names(bm)=c(paste("C",1:nrow(clum),sep=""),"all")
  bm$names=row.names(bm)
  
  par_bm=data.frame(t(bm[-ncol(bm)]),stringsAsFactors = TRUE)
  gnam=paste(names(bm)[-ncol(bm)]," (",round(csize*100,digits=1),"%",")",sep="")
  if (is.null(cluster_names))
    cluster_names <- c(paste("C",1:nrow(clum),sep=""))
    
  cluster_names <- paste(cluster_names," (",round(csize*100,digits=1),"%",")",sep="")
  cluster_names[length(gnam)] = "all"
  gnam[length(gnam)] = "all"
  par_bm$clusters=gnam
  par_bm$csize=c(csize,1/length(csize))
  
  gg_color_hue <- function(n) {
    hues = seq(15, 375, length = n + 1)
    hcl(h = hues, l = 85, c = 90)[1:n]
  }
  
  mypal=gg_color_hue(length(csize))
  mypal=c(mypal)
  par_bm$clusters <- cluster_names
  par_bm_new <- par_bm[1:length(csize),-c(p+2)] %>% tidyr::pivot_longer(-clusters, names_to='Row', values_to="value")
  pco2 <- ggplot(par_bm_new, aes(x = factor(clusters, level = cluster_names), y= value, fill = factor(Row, level = colnames(x)))) + scale_fill_Publication() +theme_Publication() +
    xlab("Clusters") + ylab("mean") +
    geom_bar(position="dodge", width=0.8,alpha=0.6, stat="identity")
  pco2$labels$fill <- "Variables"
  pco=ggparcoord(par_bm,,columns=1:p,groupColumn=p+1,scale="globalminmax",mapping = ggplot2::aes(size = 3*csize))
  pco=pco+scale_size_identity()
  pco=pco+scale_colour_manual(values=mypal)
  
  pco=pco+geom_vline(xintercept=1:p,alpha=.1) + xlab("") + ylab("mean") + scale_fill_Publication() +theme_Publication()
  
  return(pco2)
}