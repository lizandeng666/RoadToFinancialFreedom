package pers.sam.czsc.core.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import pers.sam.czsc.core.FindSegmentInterface;
import pers.sam.czsc.dto.FeatureElementDTO;
import pers.sam.czsc.dto.StrokeDTO;
import pers.sam.util.StockDateUtil;

/**
 * 线段划分--实现三
 * 修正第一第二元素的问题、修正第一第二种情况下的分法
 * 
 * @author Administrator
 *
 */
public class FindSegmentImpl3 implements FindSegmentInterface {
	
	public List<String> findSegment(List<StrokeDTO> strokeList) {
		
		List<String> resultList = new ArrayList<String>();
		
		LinkedList<StrokeDTO> processList = new LinkedList<StrokeDTO>();
		
		for(int i=0;i<strokeList.size();i++){
			processList.add((strokeList.get(i)).clone());
		}
		
		//取得一开始线段的方向
		String segmentDirection = "";
		String nextSegmentDirection="";
		StrokeDTO startStrokeDTO = strokeList.get(0);
		if(startStrokeDTO.getDirection().equals("up")){
			segmentDirection = "up";
		}else{
			segmentDirection = "down";
		}
		
		//上一线段的结束位置,初始为0
		int lastSegmentEndIndex = 0;
		
		//逐一检视特征序列
		boolean flag = true;
		while(flag){//一条线段，迭代一次。
			
			flag = false;
			
			if(lastSegmentEndIndex+3>processList.size()){
				flag = false;
				break;
			}
			
			//一线段至少有三笔，所以从三个后的元素开始检查
			for(int i=lastSegmentEndIndex+3;i<processList.size();i=i+2){
				//假设i是分界点
				
				//第一元素：第一元素就是以该假设转折点前线段的最后一个特征元素（次高）
				List<FeatureElementDTO> beforeElementList = mergeFeatureElement(
						processList, segmentDirection.equals("up") ? "down"
								: "up", lastSegmentEndIndex, i-1);
				
				FeatureElementDTO firstElement = getFirstElement(
						beforeElementList, segmentDirection);
				
				// 找到第二第三元素（特征序列合并后，即标准特征序列）
				List<FeatureElementDTO> afterElementList = mergeFeatureElement(
						processList, segmentDirection.equals("up") ? "down"
								: "up", i, processList.size()-1);
				if(afterElementList.size()<2){
					flag = false;
					break;
				}
				
				FeatureElementDTO secondElement = afterElementList.get(0);
				FeatureElementDTO thirdElement = afterElementList.get(1);
				
				// 判断i的起点是否是第二元素的极值
				if (hasHigherOrLowerPoint(secondElement, segmentDirection,
						processList,i)) {
					flag = false;
					continue;
				}
				
				//是否存在分型
				if(segmentDirection.equals("up")){
					//顶分型
					if(!(firstElement.getHigh()<secondElement.getHigh()
							&&thirdElement.getHigh()<secondElement.getHigh()
							&&thirdElement.getLow()<secondElement.getLow())){
						flag = false;
						continue;//不存在直接跳出
					}
				}else if(segmentDirection.equals("down")){
					//底分型
					if(!(firstElement.getLow()>secondElement.getLow()
							&&thirdElement.getLow()>secondElement.getLow()
							&&thirdElement.getHigh()>secondElement.getHigh())){
						flag = false;
						continue;//不存在直接跳出
					}
				}
				
				//区分第一和第二种情况
				if(!existsGapBetweenFirstAndSecondElement(segmentDirection,firstElement,processList.get(i))){
					//是第一种情况，第一元素和第二元素无缺口
					//存在并且划分成功
					flag= true;
					
					//合并第二第三元素中有包含关系的分笔，从后往前处理
					mergeProcessList(processList, thirdElement,
							segmentDirection.equals("up") ? "down" : "up");
					mergeProcessList(processList, secondElement,
							segmentDirection.equals("up") ? "down" : "up");
					
					lastSegmentEndIndex = i;
					
					nextSegmentDirection = segmentDirection.equals("up")?"down":"up";
					
					resultList.add(StockDateUtil.SDF_TIME.format(secondElement.getBeginTime()));
					System.out.println("线段端点: "+
							StockDateUtil.SDF_TIME.format(secondElement.getBeginTime())+
							" 第一种情况");
					
//					System.out.println("线段端点: "+
//							StockDateUtil.SDF_TIME.format(touchList.get(i).getStartMLine().getBeginTime())+"~"+
//							StockDateUtil.SDF_TIME.format(touchList.get(i).getEndMLine().getEndTime())+" point ");
					
				}else{
					//是第二种情况，第一元素和第二元素有缺口
					//需要见识第二特征序列是否出现分型
					String secondSegmentDirection = segmentDirection.equals("up")?"down":"up";
					
					// 获取第二特征序列
					List<FeatureElementDTO> secondElementList = mergeFeatureElement(
							processList,
							secondSegmentDirection.equals("up") ? "down" : "up",
							i, processList.size() - 1);
					
					if(secondElementList.size()<3){//少于三个，分型无从考究
						flag = false;
						continue;
					}
					
					for(int j = 1;j<secondElementList.size()-1;j++){
						FeatureElementDTO aDTO = secondElementList.get(j-1);
						FeatureElementDTO bDTO = secondElementList.get(j);
						FeatureElementDTO cDTO = secondElementList.get(j+1);
						
						// 判断第二分型的第一元素是否存在超过i的极值的点
						if (hasHigherOrLowerPoint(aDTO, segmentDirection,
								processList,i)) {
							flag = false;
							continue;
						}
						// 判断第二分型的第二元素是否存在超过i的极值的点
						if (hasHigherOrLowerPoint(bDTO, segmentDirection,
								processList,i)) {
							flag = false;
							continue;
						}
						// 判断第二分型的第三元素是否存在超过i的极值的点
						if (hasHigherOrLowerPoint(cDTO, segmentDirection,
								processList,i)) {
							flag = false;
							continue;
						}
						
						if(secondSegmentDirection.equals("down")){
							//第二特征分型是底分型
							if(bDTO.getLow()<aDTO.getLow()&&bDTO.getLow()<cDTO.getLow()
								&&bDTO.getHigh()<aDTO.getHigh()&&bDTO.getHigh()<cDTO.getHigh()){
								flag = true;
							}
						}else if(secondSegmentDirection.equals("up")){
							//第二特征分型是顶分型
							if(bDTO.getLow()>aDTO.getLow()&&bDTO.getLow()>cDTO.getLow()
								&&bDTO.getHigh()>aDTO.getHigh()&&bDTO.getHigh()>cDTO.getHigh()){
								flag = true;
							}
						}
						
						if(flag == true){
							
							//合并第二第三元素中有包含关系的分笔，从后往前处理
							mergeProcessList(
									processList,
									cDTO,
									secondSegmentDirection.equals("up") ? "down"
											: "up");
							mergeProcessList(
									processList,
									bDTO,
									secondSegmentDirection.equals("up") ? "down"
											: "up");
							
							resultList.add(StockDateUtil.SDF_TIME.format(secondElement.getBeginTime()));
							System.out.println("线段端点: "+
									StockDateUtil.SDF_TIME.format(secondElement.getBeginTime())+" 第二种情况(1)");
							
							resultList.add(StockDateUtil.SDF_TIME.format(bDTO.getBeginTime()));
							System.out.println("线段端点: "+
									StockDateUtil.SDF_TIME.format(bDTO.getBeginTime())+" 第二种情况(2)");
							
							//反查得到实际上的结束点
							lastSegmentEndIndex = findIndexByEndTime(
									processList, bDTO.getBeginTime());
							nextSegmentDirection = segmentDirection;
							break;
						}
					}
				}
				
				if(flag == true){//找到线段,第一种情况新线段反向，第二种请，新线段与原线段同向
					segmentDirection = nextSegmentDirection;
					break;
				}
			}
			
			if(flag == false){//
				break;
			}
		}
		
		//输出结果
//		for(int i = 0;i<resultIndexList.size();i++){
//			int resutIndex = (Integer)resultIndexList.get(i);
//			
//			TouchDTO touchDTO = touchList.get(resutIndex);
//			System.out.println("线段于 "+
//					StockDateUtil.SDF_TIME.format(touchDTO.getStartMLine().getBeginTime())+"~"+
//					StockDateUtil.SDF_TIME.format(touchDTO.getEndMLine().getEndTime())+" point ");
//		}
		return resultList;
		
	}
	
	/**
	 * 根据线段的结束时间，反查结束点的序号
	 * @param processList
	 * @param endTime
	 * @return
	 */
	public Integer findIndexByEndTime(LinkedList<StrokeDTO> processList,Date endTime){
		
		for(int i = processList.size()-1;i<processList.size();i--){
			StrokeDTO dto = processList.get(i);
			if(dto.getEndMLine().getEndTime().compareTo(endTime)>=0
					&&dto.getStartMLine().getBeginTime().compareTo(endTime)<=0){
				return new Integer(i);
			}
		}
		return null;
	}
	
	/**
	 * 判断某合并后的特征序列中，是否有比i元素更高或更低的点
	 * 
	 * @param secondElement
	 * @param segmentDirection
	 * @return
	 */
	public boolean hasHigherOrLowerPoint(FeatureElementDTO secondElement,
			String segmentDirection, LinkedList<StrokeDTO> processList,Integer compareIndex) {
		
		StrokeDTO compareStroke = processList.get(compareIndex);

		Integer firstIndex = secondElement.getStrokeIndexList().get(0);
		Integer lastIndex = secondElement.getStrokeIndexList().get(
				secondElement.getStrokeIndexList().size() - 1);
		
		for(int i = firstIndex;i<=lastIndex;i++){
			
			if(i==compareIndex){
				continue;
			}
			
			StrokeDTO nStroke = processList.get(i);
			
			if("up".equals(segmentDirection)
					&&nStroke.getHigh()>compareStroke.getHigh()){
				//线段方向向上，并且合并元素中又更高的点
				return true;
			}else if("down".equals(segmentDirection)
					&&nStroke.getLow()<compareStroke.getLow()){
				//线段方向向上，并且合并元素中有更低的点
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 合并第二第三元素中有包含关系的分笔，从后往前处理
	 */
	public static void mergeProcessList(LinkedList<StrokeDTO> processList,FeatureElementDTO featureElement,String featureDirection){
		
		List<Integer> strokeIndexList = featureElement.getStrokeIndexList();
		
		int startIndex = strokeIndexList.get(0);
		int endIndex = strokeIndexList.get(strokeIndexList.size()-1);
		
		StrokeDTO headDTO = processList.get(startIndex);
		StrokeDTO tailDTO = processList.get(endIndex);
		
		StrokeDTO newDTO = new StrokeDTO();
		newDTO.setDirection(featureDirection);
		newDTO.setStartMLine(headDTO.getStartMLine());
		newDTO.setEndMLine(tailDTO.getEndMLine());
		
		//从后往前删
		for(int i = endIndex;i>= startIndex;i--){
			processList.remove(i);
		}
		
		processList.add(startIndex, newDTO);
		
	}
	
	/*
	 * 判断第一第二元素间是否有缺口（不考虑合并关系）
	 */
	private boolean existsGapBetweenFirstAndSecondElement(
			String segmentDirection, FeatureElementDTO firstElement,
			StrokeDTO secondDTO) {
		return (
			(segmentDirection.equals("up")&&firstElement.getHigh()<Math.min(secondDTO.getEndMLine().getLow(), secondDTO.getStartMLine().getLow()))||
			(segmentDirection.equals("down")&&firstElement.getLow()>Math.max(secondDTO.getEndMLine().getHigh(), secondDTO.getStartMLine().getHigh()))
			);
	}
	
	/**
	 * 获取第一元素
	 * 选择次高点/次低点
	 * 
	 * 
	 * @param beforeElementList
	 * @return
	 */
	public FeatureElementDTO getFirstElement(List<FeatureElementDTO> beforeElementList,String segmentDirection){
		
		FeatureElementDTO firstElement = beforeElementList
				.get(beforeElementList.size() - 1);
		
		if(segmentDirection.equals("up")){
			//向上的线段，特征序列是向下笔，取最高点的元素
			for(int i = beforeElementList.size()-1;i>=0;i--){
				FeatureElementDTO dto = beforeElementList.get(i);
				if(dto.getHigh()>firstElement.getHigh()){
					firstElement = dto;
				}
			}
		}else if(segmentDirection.equals("down")){
			//向下的线段，特征序列是向上笔，取最低点的元素
			for(int i = beforeElementList.size()-1;i>=0;i--){
				FeatureElementDTO dto = beforeElementList.get(i);
				if(dto.getLow()<firstElement.getLow()){
					firstElement = dto;
				}
			}
		}
		return firstElement;
	}
	
	/**
	 * 处理特征序列的合并关系
	 * 
	 * @return
	 */
	public static List<FeatureElementDTO> mergeFeatureElement(List<StrokeDTO> strokeList,
			String featureDirection, int startIndex, int endIndex) {
		
		//由分笔中抓出特征序列
		List<FeatureElementDTO> featureElementList = new ArrayList<FeatureElementDTO>();
		for(int i = startIndex;i<=endIndex;i++){
			StrokeDTO strokeDTO = strokeList.get(i);
			if(strokeDTO.getDirection().equals(featureDirection)){
				FeatureElementDTO elementDTO = new FeatureElementDTO();
				elementDTO.setBeginTime(strokeDTO.getStartMLine().getBeginTime());
				elementDTO.setEndTime(strokeDTO.getEndMLine().getEndTime());
				if(strokeDTO.getDirection().equals("up")){
					elementDTO.setHigh(strokeDTO.getEndMLine().getHigh());
					elementDTO.setLow(strokeDTO.getStartMLine().getLow());
				}else if(strokeDTO.getDirection().equals("down")){
					elementDTO.setHigh(strokeDTO.getStartMLine().getHigh());
					elementDTO.setLow(strokeDTO.getEndMLine().getLow());	
				}
				elementDTO.getStrokeIndexList().add(i);
				featureElementList.add(elementDTO);
			}
		}
		
		boolean flag = true;
		while(flag){
			FeatureElementDTO  mergeDTO = new FeatureElementDTO();
			List<FeatureElementDTO> headList = new ArrayList<FeatureElementDTO>();
			List<FeatureElementDTO> tailLsit = new ArrayList<FeatureElementDTO>();
			
			flag = false;
			for(int i = 1;i<featureElementList.size();i++){
				FeatureElementDTO lastDTO = featureElementList.get(i-1);
				FeatureElementDTO thisDTO = featureElementList.get(i);
				
				//包含关系
				if((lastDTO.getHigh()>=thisDTO.getHigh()&&lastDTO.getLow()<=thisDTO.getLow())
					||(thisDTO.getHigh()>=lastDTO.getHigh()&&thisDTO.getLow()<=lastDTO.getLow())
					){
					
					//合并
					mergeDTO.setBeginTime(lastDTO.getBeginTime());
					mergeDTO.setEndTime(thisDTO.getEndTime());
					mergeDTO.getStrokeIndexList().addAll(lastDTO.getStrokeIndexList());//添加前序列分笔编号
					mergeDTO.getStrokeIndexList().addAll(thisDTO.getStrokeIndexList());//添加后序列分笔编号
					
					if(featureDirection.equals("up")){
						mergeDTO.setHigh(Math.min(lastDTO.getHigh(), thisDTO.getHigh()));
						mergeDTO.setLow(Math.min(lastDTO.getLow(), thisDTO.getLow()));
					}else if(featureDirection.equals("down")){
						mergeDTO.setHigh(Math.max(lastDTO.getHigh(), thisDTO.getHigh()));
						mergeDTO.setLow(Math.max(lastDTO.getLow(), thisDTO.getLow()));
					}
					
					flag=true;
					if(i!=0){
						headList = featureElementList.subList(0, i-1);
					}
					
					if(i!=featureElementList.size()-1){
						tailLsit = featureElementList.subList(i+1, featureElementList.size());
					}
					break;
				}
			}
			
			if(flag){
				featureElementList = new ArrayList<FeatureElementDTO>();
				featureElementList.addAll(headList);
				featureElementList.add(mergeDTO);
				featureElementList.addAll(tailLsit);
			}else{
				flag = false;
			}
		}
		
		return featureElementList;
	}
}
