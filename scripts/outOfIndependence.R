outOfIndependence=function(data,Gvec,labs,nolabs=FALSE,fixmarg=TRUE,firstfew=0,segSize=4,textSize=4,myleftmarg = 0.5, myrightmarg = 0.5, cluster_names){
  value = NULL
  newplace = NULL
  lbls = NULL
  glab = cluster_names
  data=as.data.frame(lapply(data,as.factor))
  data= data.frame(tab.disjonctif(data),stringsAsFactors = TRUE)
  K=max(Gvec)
  
  C=matrix(0,nrow(data),max(Gvec))
  
  for(j in 1:max(Gvec)){
    C[which(Gvec==j),j]=1
  }
  
  P=t(data) %*% C
  n=nrow(data)
  
  P=P/sum(P)
  
  c=apply(P,2,sum)
  c=t(t(c))
  r=apply(P,1,sum)
  r=t(t(r))
  
  invsqDc=diag(as.vector(1/sqrt(c)))
  invsqDr=diag(as.vector(1/sqrt(r)))
  eP = r%*% t(c)
  devP=invsqDr %*% (P-eP) %*% invsqDc
  
  dfP=list()
  sortOp=list()
  bp=list()
  
  colorPal=c("#fdb462","#7fc97f","#386cb0","#ef3b2c","#662506","#a6cee3","#fb9a99","#984ea3","#ffff33","#fdb462","#7fc97f","#386cb0","#ef3b2c","#662506","#a6cee3","#fb9a99","#984ea3","#ffff33","#fdb462","#7fc97f","#386cb0","#ef3b2c","#662506","#a6cee3","#fb9a99","#984ea3","#ffff33","#fdb462","#7fc97f","#386cb0","#ef3b2c","#662506","#a6cee3","#fb9a99","#984ea3","#ffff33","#fdb462","#7fc97f","#386cb0","#ef3b2c","#662506","#a6cee3","#fb9a99","#984ea3","#ffff33")
  for(jj in 1:K){
    dfP[[jj]]=data.frame(value=devP[,jj]*sqrt(n),place=1:nrow(devP),lbls=labs,stringsAsFactors = TRUE)
    sortOp[[jj]]=sort(abs(dfP[[jj]]$value),decreasing=TRUE,index.return=TRUE)
    
    dfP[[jj]]=dfP[[jj]][sortOp[[jj]]$ix,]
    dfP[[jj]]$newplace=nrow(devP):1
    xran=c(min(dfP[[jj]]$value)-myleftmarg,max(dfP[[jj]]$value)+myrightmarg)
    if(firstfew>0){
      dfP[[jj]]=dfP[[jj]][1:firstfew,]
      dfP[[jj]]$newplace=firstfew:1
    }
    
    bbp=ggplot(data=dfP[[jj]], aes(x=value,y=newplace),labels=lbls)
    
    if(fixmarg==TRUE){
      bbp=bbp+geom_segment(data=dfP[[jj]],aes(x=0,xend=value,y=newplace,yend=newplace),colour=colorPal[jj],size=segSize,alpha=.35)
      bbp=bbp+theme(legend.position="none") + ylab("attribute rank")+xlab("standardized residual") +coord_cartesian(xlim = xran)
      if(firstfew==0){
        bbp=bbp+theme(axis.line=element_blank(),axis.ticks = element_blank())
      }
    }
    else{
      bbp=bbp+geom_segment(data=dfP[[jj]],aes(x=-0,xend=value,y=newplace,yend=newplace),colour=colorPal[jj],size=segSize,alpha=.35)
      bbp=bbp+theme(legend.position="none")+xlab("attribute rank")+ylab("standardized residual") +xlim(xran)
      if(firstfew==0){bbp=bbp+theme(axis.line=element_blank(),axis.ticks = element_blank())}
    }
    if(nolabs==FALSE){
      bbp=bbp+geom_text(data=dfP[[jj]],aes(label=lbls),size=textSize)
    }
    bp[[jj]]=bbp
  }
  out=list()
  out$G=bp
  
  out
}